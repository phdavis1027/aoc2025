#include <stdio.h>
#include <stdint.h>
#include <immintrin.h>
#include <inttypes.h>

static FILE* current_file = NULL;

int32_t open_file(const char* path) {
    if (current_file) fclose(current_file);
    current_file = fopen(path, "r");
    return current_file ? 0 : -1;
}

// Reads into caller-provided buffer, returns bytes read or -1 on EOF
int64_t read_line(char* buf, int64_t buf_len) {
    if (!current_file) return -1;
    if (!fgets(buf, buf_len, current_file)) return -1;
    int64_t len = 0;
    while (len < buf_len && buf[len] && buf[len] != '\n') len++;
    return len;
}

void close_file(void) {
    if (current_file) {
        fclose(current_file);
        current_file = NULL;
    }
}

static __m256i pipe_vec;
static __m256i caret_vec;
static __m256i dot_vec;

void init_scan_vecs(void) {
    pipe_vec = _mm256_set1_epi8('|');
    caret_vec = _mm256_set1_epi8('^');
    dot_vec = _mm256_set1_epi8('.');
}

// static int debug_line_count = 0;

int32_t scan_line_pair(
    const char* prev_buf,
    char* curr_buf,
    int64_t lineLen,
    uint64_t* out_pipe_mask,
    uint64_t* out_caret_mask
) {
    out_pipe_mask[0] = out_pipe_mask[1] = out_pipe_mask[2] = out_pipe_mask[3] = 0;
    out_caret_mask[0] = out_caret_mask[1] = out_caret_mask[2] = out_caret_mask[3] = 0;


    for (int64_t chunk = 0; chunk * 32 < lineLen; ++chunk) {
        int64_t offset = chunk * 32;
        int word_idx = chunk >> 1;
        int shift = (chunk & 1) * 32;

        __m256i prev_data = _mm256_loadu_si256((const __m256i*)(prev_buf + offset));
        __m256i curr_data = _mm256_loadu_si256((const __m256i*)(curr_buf + offset));

        __m256i pipe_cmp = _mm256_cmpeq_epi8(prev_data, pipe_vec);
        __m256i caret_cmp = _mm256_cmpeq_epi8(curr_data, caret_vec);
        __m256i dot_cmp = _mm256_cmpeq_epi8(curr_data, dot_vec);

        __m256i fill_mask = _mm256_and_si256(pipe_cmp, dot_cmp);
		// Flood | -- write '|' to every byte in current data that was '.' before
        __m256i new_data = _mm256_blendv_epi8(curr_data, pipe_vec, fill_mask);
        _mm256_storeu_si256((__m256i*)(curr_buf + offset), new_data);

        uint32_t pipe_bits = (uint32_t)_mm256_movemask_epi8(pipe_cmp);
        uint32_t caret_bits = (uint32_t)_mm256_movemask_epi8(caret_cmp);
        out_pipe_mask[word_idx] |= ((uint64_t)pipe_bits) << shift;
        out_caret_mask[word_idx] |= ((uint64_t)caret_bits) << shift;
    }

    uint64_t aligned[4] = {
        out_pipe_mask[0] & out_caret_mask[0],
        out_pipe_mask[1] & out_caret_mask[1],
        out_pipe_mask[2] & out_caret_mask[2],
        out_pipe_mask[3] & out_caret_mask[3]
    };

    uint64_t neighbors[4] = {
        (aligned[0] >> 1) | (aligned[0] << 1) | (aligned[1] << 63),
        (aligned[1] >> 1) | (aligned[1] << 1) | (aligned[0] >> 63) | (aligned[2] << 63),
        (aligned[2] >> 1) | (aligned[2] << 1) | (aligned[1] >> 63) | (aligned[3] << 63),
        (aligned[3] >> 1) | (aligned[3] << 1) | (aligned[2] >> 63)
    };

    for (int w = 0; w < 4; w++) {
        uint64_t m = neighbors[w] & ~out_caret_mask[w];
        while (m) {
            int idx = w * 64 + __builtin_ctzll(m);
            curr_buf[idx] = '|';
            m &= (m - 1);
        }
    }

    return __builtin_popcountll(aligned[0]) + __builtin_popcountll(aligned[1]) +
           __builtin_popcountll(aligned[2]) + __builtin_popcountll(aligned[3]);
}
