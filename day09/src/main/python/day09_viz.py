"""
Day09 Algorithm Visualization using Manim

Visualizes the Part Two algorithm from Day09.kt:
- Left panel: Cartesian (x,y) grid with polygon and rectangle checks
- Right panel: Chebyshev (u,v) space with frontier expansion
- Bottom: Candidate heap tree

Run with: manim -pql day09_viz.py Day09Visualization
"""

from manim import *
import heapq
from dataclasses import dataclass
from pathlib import Path
import logging

logger = logging.getLogger("day09_viz")

# Configuration
DATA_FILE = "../../input.txt"  # Change to "../../test.txt" for small test
ANIMATION_SPEED = 0.8  # Seconds per animation (intro phase)
PAUSE_DURATION = 0.5  # Seconds to pause between steps (intro phase)
MAX_HEAP_NODES = 15  # Max nodes shown in heap tree
EXPLANATION_FONT_SIZE = 16  # Font size for explanatory text
EXPLANATION_READ_TIME = 3.0  # Time to display explanatory text

# Two-phase animation: slow intro, then fast bulk processing
INTRO_ITERATIONS = 3  # Number of fully-animated iterations with explanations
FAST_ANIMATION_SPEED = 0.1  # Seconds per animation in fast mode (min ~0.067 for 15fps)
FAST_PAUSE_DURATION = 0.0  # Pause duration in fast mode (0 = no wait)
ANIMATE_EVERY_N_FAST = 20  # In fast mode, only animate every Nth candidate
SHOW_PROGRESS_EVERY = 100  # Show progress indicator every N iterations

# Large input optimizations - auto-enabled when n > LARGE_INPUT_THRESHOLD
LARGE_INPUT_THRESHOLD = 100  # vertices
LARGE_ANIMATE_EVERY_N = 1000  # Only show every 1000th candidate
LARGE_ANIMATION_SPEED = 0.03  # Minimal animation time
MAX_ANIMATED_CANDIDATES = 100  # Hard cap on animated candidates

# Layout positions
LEFT_PANEL_CENTER = np.array([-3.5, 0.8, 0])
RIGHT_PANEL_CENTER = np.array([3.5, 0.8, 0])
HEAP_CENTER = np.array([0, -2.8, 0])

# Colors
COLOR_OUTSIDE = "#000000"
COLOR_INSIDE = "#0a3d3d"
COLOR_VERTEX = "#00ff00"
COLOR_EDGE = "#00cc00"
COLOR_CANDIDATE = "#ffff00"
COLOR_VALID = "#00ff00"
COLOR_INVALID = "#ff0000"
COLOR_HEAP_BG = "#1a1a2e"
COLOR_U_FRONTIER = "#ff8c00"  # Orange
COLOR_V_FRONTIER = "#9932cc"  # Purple


def parse_polygon(filename: str) -> list[tuple[int, int]]:
    """Load polygon points from file."""
    script_dir = Path(__file__).parent
    filepath = script_dir / filename
    points = []
    with open(filepath) as f:
        for line in f:
            line = line.strip()
            if line:
                x, y = line.split(",")
                points.append((int(x), int(y)))
    return points


def compress_coordinates(points: list[tuple[int, int]]) -> tuple[list[int], list[int], dict[int, int], dict[int, int]]:
    """Build sorted unique coordinate arrays and index mappings."""
    xs = sorted(set(p[0] for p in points))
    ys = sorted(set(p[1] for p in points))
    x_index = {v: i for i, v in enumerate(xs)}
    y_index = {v: i for i, v in enumerate(ys)}
    return xs, ys, x_index, y_index


def build_vertical_edges(points: list[tuple[int, int]]) -> dict[int, list[tuple[int, int]]]:
    """Build vertical edges for ray casting."""
    n = len(points)
    vertical_edges: dict[int, list[tuple[int, int]]] = {}
    for i in range(n):
        x1, y1 = points[i]
        x2, y2 = points[(i + 1) % n]
        if x1 == x2:
            if x1 not in vertical_edges:
                vertical_edges[x1] = []
            vertical_edges[x1].append((min(y1, y2), max(y1, y2)))
    return vertical_edges


def is_cell_inside(cell_x: int, cell_y: int, xs: list[int], ys: list[int],
                   vertical_edges: dict[int, list[tuple[int, int]]]) -> bool:
    """Check if cell interior is inside polygon using ray casting."""
    px = xs[cell_x]
    py = ys[cell_y]
    crossings = 0
    for x, ranges in vertical_edges.items():
        if x > px:
            for min_y, max_y in ranges:
                if py >= min_y and py < max_y:
                    crossings += 1
    return crossings % 2 == 1


def build_inside_grid(xs: list[int], ys: list[int],
                      vertical_edges: dict[int, list[tuple[int, int]]]) -> list[list[bool]]:
    """Build 2D grid of inside/outside status."""
    num_cells_x = len(xs) - 1
    num_cells_y = len(ys) - 1
    return [[is_cell_inside(i, j, xs, ys, vertical_edges)
             for j in range(num_cells_y)]
            for i in range(num_cells_x)]


def build_prefix_sums(inside: list[list[bool]]) -> list[list[int]]:
    """Build 2D prefix sums for efficient rectangle queries."""
    num_cells_x = len(inside)
    num_cells_y = len(inside[0]) if inside else 0
    prefix = [[0] * (num_cells_y + 1) for _ in range(num_cells_x + 1)]
    for i in range(num_cells_x):
        for j in range(num_cells_y):
            prefix[i + 1][j + 1] = (prefix[i][j + 1] + prefix[i + 1][j] - prefix[i][j] +
                                    (1 if inside[i][j] else 0))
    return prefix


def count_inside(i1: int, j1: int, i2: int, j2: int, prefix: list[list[int]]) -> int:
    """Count inside cells in rectangle [i1, i2) x [j1, j2)."""
    return prefix[i2][j2] - prefix[i1][j2] - prefix[i2][j1] + prefix[i1][j1]


def chebyshev_transform(x: int, y: int) -> tuple[int, int]:
    """Transform to Chebyshev coordinates (u, v)."""
    return (x + y, x - y)


def manhattan_distance(p1: tuple[int, int], p2: tuple[int, int]) -> int:
    """Calculate Manhattan distance between two points."""
    return abs(p1[0] - p2[0]) + abs(p1[1] - p2[1])


@dataclass
class Candidate:
    """Represents a candidate pair for rectangle check."""
    distance: int
    i: int
    j: int
    source: str  # 'u' or 'v'
    left: int
    right: int

    def __lt__(self, other):
        return self.distance > other.distance  # Max-heap


class Day09Visualization(Scene):
    """Main visualization scene with side-by-side Cartesian and Chebyshev panels."""

    def show_explanation(self, lines: list[str], position=None, wait_time=EXPLANATION_READ_TIME) -> VGroup:
        """Display explanatory text and wait for user to read it."""
        if position is None:
            position = np.array([0, -3.5, 0])

        text_group = VGroup()
        for i, line in enumerate(lines):
            text = Text(line, font_size=EXPLANATION_FONT_SIZE, color=YELLOW_A)
            text.move_to(position + DOWN * i * 0.35)
            text_group.add(text)

        self.play(FadeIn(text_group), run_time=0.5)
        self.wait(wait_time)
        return text_group

    def hide_explanation(self, text_group: VGroup):
        """Fade out explanatory text."""
        self.play(FadeOut(text_group), run_time=0.3)

    def update_progress(self, iteration: int, current_distance: int):
        """Show progress indicator during fast mode."""
        if self.progress_text:
            self.remove(self.progress_text)
        pct = (self.candidates_considered / self.total_candidates * 100) if self.total_candidates > 0 else 0
        self.progress_text = Text(
            f"Candidates: {self.candidates_considered}/{self.total_candidates} ({pct:.1f}%) | Distance: {current_distance} | Heap: {len(self.heap)}",
            font_size=14, color=GRAY_A
        )
        self.progress_text.to_corner(DL, buff=0.2)
        self.add(self.progress_text)
        # Quick heap update without animation
        self.update_heap_display()
        self.update_all_frontier_displays()

    def create_stats_display(self):
        """Create persistent stats display for large input mode."""
        self.stats_bg = Rectangle(
            width=5.5, height=1.2,
            fill_color=COLOR_HEAP_BG, fill_opacity=0.9,
            stroke_color=WHITE, stroke_width=1
        )
        self.stats_bg.to_corner(DL, buff=0.2)
        self.add(self.stats_bg)
        self.stats_text = None
        self.animated_count = 0  # Track number of animated candidates

    def update_stats_display(self, current_distance: int, animate: bool = False):
        """Update stats display - no animation for speed."""
        if self.stats_text:
            self.remove(self.stats_text)

        pct = (self.candidates_considered / self.total_candidates * 100) if self.total_candidates > 0 else 0

        lines = [
            f"Checked: {self.candidates_considered:,} / {self.total_candidates:,} ({pct:.1f}%)",
            f"Distance: {current_distance:,}  |  Heap: {len(self.heap):,}",
            f"Animated: {self.animated_count} / {MAX_ANIMATED_CANDIDATES}"
        ]

        self.stats_text = VGroup()
        for i, line in enumerate(lines):
            text = Text(line, font_size=14, color=WHITE)
            text.move_to(self.stats_bg.get_center() + UP * 0.3 - DOWN * i * 0.35)
            self.stats_text.add(text)

        self.add(self.stats_text)

    def get_animation_speed(self) -> float:
        """Return current animation speed based on mode."""
        return FAST_ANIMATION_SPEED if self.fast_mode else ANIMATION_SPEED

    def get_pause_duration(self) -> float:
        """Return current pause duration based on mode."""
        return FAST_PAUSE_DURATION if self.fast_mode else PAUSE_DURATION

    def maybe_wait(self, duration: float):
        """Wait only if duration is positive."""
        if duration > 0:
            self.wait(duration)

    @property
    def is_large_input(self) -> bool:
        """Check if input is large enough to enable optimizations."""
        return len(self.points) > LARGE_INPUT_THRESHOLD

    def illustrate_vector_decomposition(self):
        """
        Illustrate why only min/max combinations are optimal using vector addition.
        Shows that u_p - u_q = u_p + (-u_q) is maximized when we pick max(u) and min(u).
        Uses LaTeX and toggling to demonstrate the effect of each term.
        """
        # Create overlay
        illustration_bg = Rectangle(
            width=14, height=8,
            fill_color=BLACK, fill_opacity=0.95,
            stroke_color=WHITE, stroke_width=2
        )
        illustration_bg.move_to(ORIGIN)
        self.play(FadeIn(illustration_bg), run_time=0.5)

        title = MathTex(r"\text{Vector Decomposition: Why Only Extremes Matter}", font_size=36)
        title.move_to(UP * 3.5)
        self.play(Write(title), run_time=0.5)

        # Show the key equation
        eq1 = MathTex(r"u_p - u_q", r"=", r"u_p", r"+", r"(-u_q)", font_size=42)
        eq1.move_to(UP * 2.5)
        eq1[2].set_color(GREEN)  # u_p
        eq1[4].set_color(RED)    # (-u_q)
        self.play(Write(eq1), run_time=1)
        self.wait(1)

        # Explain the two terms
        term_explain = VGroup(
            MathTex(r"\text{To maximize this sum, we want:}", font_size=28),
            MathTex(r"\bullet\;", r"u_p", r"\text{ as large as possible (positive contribution)}", font_size=24),
            MathTex(r"\bullet\;", r"(-u_q)", r"\text{ as large as possible} \Rightarrow u_q \text{ as small as possible}", font_size=24),
        )
        term_explain[1][1].set_color(GREEN)
        term_explain[2][1].set_color(RED)
        term_explain.arrange(DOWN, buff=0.3, aligned_edge=LEFT)
        term_explain.move_to(UP * 1.2)
        self.play(Write(term_explain), run_time=1.5)
        self.wait(2)

        # Vector visualization area
        vec_origin = DOWN * 1.5 + LEFT * 3
        scale = 0.35

        # Available points
        points = {4: GREEN, 2: BLUE, -1: ORANGE, -3: RED}

        # === PART 1: Fix u_p, vary u_q to show effect of the negative term ===
        part1_title = MathTex(r"\text{Fix } u_p = 4 \text{, vary } u_q \text{:}", font_size=28, color=YELLOW)
        part1_title.move_to(DOWN * 0.3)
        self.play(FadeOut(term_explain), Write(part1_title), run_time=0.5)

        u_p_fixed = 4

        # Draw fixed u_p vector
        vec_p_end = vec_origin + RIGHT * u_p_fixed * scale
        vec_p = Arrow(vec_origin, vec_p_end, color=GREEN, stroke_width=5, buff=0,
                     max_tip_length_to_length_ratio=0.12)
        vec_p_label = MathTex(r"u_p = 4", font_size=24, color=GREEN)
        vec_p_label.next_to(vec_p, UP, buff=0.15)
        self.play(GrowArrow(vec_p), Write(vec_p_label), run_time=0.5)

        # Toggle through different u_q values
        u_q_values = [2, -1, -3]  # From worst to best for maximizing
        prev_objects = []

        for i, u_q in enumerate(u_q_values):
            neg_u_q = -u_q
            vec_q_end = vec_p_end + RIGHT * neg_u_q * scale
            total = u_p_fixed + neg_u_q

            # Second vector
            vec_q = Arrow(vec_p_end, vec_q_end, color=points[u_q], stroke_width=5, buff=0,
                         max_tip_length_to_length_ratio=0.12)
            vec_q_label = MathTex(f"-u_q = -({u_q}) = {neg_u_q}", font_size=24, color=points[u_q])
            vec_q_label.next_to(vec_q, UP if neg_u_q >= 0 else DOWN, buff=0.15)

            # Resultant
            result_vec = Arrow(vec_origin, vec_q_end, color=YELLOW, stroke_width=7, buff=0,
                              max_tip_length_to_length_ratio=0.08)
            result_label = MathTex(f"\\text{{Total}} = {u_p_fixed} + ({neg_u_q}) = {total}", font_size=28, color=YELLOW)
            result_label.move_to(DOWN * 3)

            # Insight text
            if u_q == 2:
                insight = MathTex(r"u_q = 2 \text{ (positive)} \Rightarrow -u_q = -2 \text{ subtracts from total}", font_size=22, color=GRAY_A)
            elif u_q == -1:
                insight = MathTex(r"u_q = -1 \text{ (negative)} \Rightarrow -u_q = 1 \text{ adds to total}", font_size=22, color=GRAY_A)
            else:
                insight = MathTex(r"u_q = -3 \text{ (most negative)} \Rightarrow -u_q = 3 \text{ adds most!}", font_size=22, color=GREEN)
            insight.move_to(DOWN * 3.6)

            if prev_objects:
                self.play(
                    *[FadeOut(obj) for obj in prev_objects],
                    GrowArrow(vec_q), Write(vec_q_label),
                    GrowArrow(result_vec), Write(result_label),
                    Write(insight),
                    run_time=0.6
                )
            else:
                self.play(
                    GrowArrow(vec_q), Write(vec_q_label),
                    GrowArrow(result_vec), Write(result_label),
                    Write(insight),
                    run_time=0.6
                )

            self.wait(2)
            prev_objects = [vec_q, vec_q_label, result_vec, result_label, insight]

        # Clean up part 1
        self.play(
            *[FadeOut(obj) for obj in prev_objects],
            FadeOut(vec_p), FadeOut(vec_p_label), FadeOut(part1_title),
            run_time=0.5
        )

        # === PART 2: Fix u_q, vary u_p to show effect of the positive term ===
        part2_title = MathTex(r"\text{Fix } u_q = -3 \text{, vary } u_p \text{:}", font_size=28, color=YELLOW)
        part2_title.move_to(DOWN * 0.3)
        self.play(Write(part2_title), run_time=0.5)

        u_q_fixed = -3
        neg_u_q_fixed = 3

        # Toggle through different u_p values
        u_p_values = [-1, 2, 4]  # From worst to best
        prev_objects = []

        for u_p in u_p_values:
            total = u_p + neg_u_q_fixed

            # First vector: u_p
            vec_p_end = vec_origin + RIGHT * u_p * scale
            vec_p = Arrow(vec_origin, vec_p_end, color=points[u_p], stroke_width=5, buff=0,
                         max_tip_length_to_length_ratio=0.12)
            vec_p_label = MathTex(f"u_p = {u_p}", font_size=24, color=points[u_p])
            vec_p_label.next_to(vec_p, UP if u_p >= 0 else DOWN, buff=0.15)

            # Second vector: -u_q (fixed)
            vec_q_end = vec_p_end + RIGHT * neg_u_q_fixed * scale
            vec_q = Arrow(vec_p_end, vec_q_end, color=RED, stroke_width=5, buff=0,
                         max_tip_length_to_length_ratio=0.12)
            vec_q_label = MathTex(r"-u_q = 3", font_size=24, color=RED)
            vec_q_label.next_to(vec_q, UP, buff=0.15)

            # Resultant
            result_vec = Arrow(vec_origin, vec_q_end, color=YELLOW, stroke_width=7, buff=0,
                              max_tip_length_to_length_ratio=0.08)
            result_label = MathTex(f"\\text{{Total}} = {u_p} + 3 = {total}", font_size=28, color=YELLOW)
            result_label.move_to(DOWN * 3)

            # Insight
            if u_p == -1:
                insight = MathTex(r"u_p = -1 \text{ (negative)} \Rightarrow \text{subtracts from total}", font_size=22, color=GRAY_A)
            elif u_p == 2:
                insight = MathTex(r"u_p = 2 \text{ (positive)} \Rightarrow \text{adds to total}", font_size=22, color=GRAY_A)
            else:
                insight = MathTex(r"u_p = 4 \text{ (most positive)} \Rightarrow \text{adds most!}", font_size=22, color=GREEN)
            insight.move_to(DOWN * 3.6)

            if prev_objects:
                self.play(
                    *[FadeOut(obj) for obj in prev_objects],
                    GrowArrow(vec_p), Write(vec_p_label),
                    GrowArrow(vec_q), Write(vec_q_label),
                    GrowArrow(result_vec), Write(result_label),
                    Write(insight),
                    run_time=0.6
                )
            else:
                self.play(
                    GrowArrow(vec_p), Write(vec_p_label),
                    GrowArrow(vec_q), Write(vec_q_label),
                    GrowArrow(result_vec), Write(result_label),
                    Write(insight),
                    run_time=0.6
                )

            self.wait(2)
            prev_objects = [vec_p, vec_p_label, vec_q, vec_q_label, result_vec, result_label, insight]

        # Clean up part 2
        self.play(*[FadeOut(obj) for obj in prev_objects], FadeOut(part2_title), run_time=0.5)

        # === CONCLUSION ===
        conclusion = VGroup(
            MathTex(r"\text{Therefore, to maximize } u_p - u_q = u_p + (-u_q) \text{:}", font_size=28),
            MathTex(r"\bullet\;", r"u_p = \max(u)", r"\text{ contributes the largest positive term}", font_size=24),
            MathTex(r"\bullet\;", r"u_q = \min(u)", r"\text{ makes } -u_q \text{ the largest positive term}", font_size=24),
            MathTex(r"\Rightarrow \text{Only extreme points can be optimal!}", font_size=30, color=GREEN),
        )
        conclusion[1][1].set_color(GREEN)
        conclusion[2][1].set_color(RED)
        conclusion.arrange(DOWN, buff=0.4, aligned_edge=LEFT)
        conclusion.move_to(DOWN * 0.5)

        self.play(Write(conclusion), run_time=2)
        self.wait(4)

        # Clean up
        self.play(
            FadeOut(illustration_bg), FadeOut(title), FadeOut(eq1),
            FadeOut(conclusion),
            run_time=0.8
        )

    def construct(self):
        # Initialize mode tracking
        self.fast_mode = False
        self.progress_text = None
        self.candidates_considered = 0
        self.total_candidates = 0
        self.estimated_animations = 0

        # Load data
        self.points = parse_polygon(DATA_FILE)
        n = len(self.points)

        # Build data structures
        self.xs, self.ys, self.x_index, self.y_index = compress_coordinates(self.points)
        self.vertical_edges = build_vertical_edges(self.points)
        self.inside = build_inside_grid(self.xs, self.ys, self.vertical_edges)
        self.prefix = build_prefix_sums(self.inside)

        # Chebyshev coordinates
        self.cheb_points = [chebyshev_transform(p[0], p[1]) for p in self.points]
        self.by_u = sorted(range(n), key=lambda i: self.cheb_points[i][0])
        self.by_v = sorted(range(n), key=lambda i: self.cheb_points[i][1])

        # Calculate scaling for left panel (Cartesian grid)
        num_cells_x = len(self.xs) - 1
        num_cells_y = len(self.ys) - 1
        max_panel_size = 5.0
        self.cell_size = min(max_panel_size / num_cells_x, max_panel_size / num_cells_y)
        grid_width = num_cells_x * self.cell_size
        grid_height = num_cells_y * self.cell_size
        self.grid_offset = LEFT_PANEL_CENTER + np.array([-grid_width / 2, -grid_height / 2, 0])

        # Calculate scaling for right panel (Chebyshev space)
        us = [p[0] for p in self.cheb_points]
        vs = [p[1] for p in self.cheb_points]
        u_range = max(us) - min(us)
        v_range = max(vs) - min(vs)
        self.cheb_scale = max_panel_size / max(u_range, v_range) * 0.8
        self.u_min, self.v_min = min(us), min(vs)
        self.u_max, self.v_max = max(us), max(vs)

        # Create left panel (Cartesian)
        self.create_cartesian_panel()

        # Create right panel (Chebyshev)
        self.create_chebyshev_panel()

        # Create frontier displays
        self.create_frontier_displays()

        # Create heap display (skip visual for large inputs)
        self.heap_group = VGroup()
        if not self.is_large_input:
            self.heap_bg = self.create_heap_background()
            self.add(self.heap_bg)
            self.add(self.heap_group)

        # Initialize algorithm state
        self.heap: list[Candidate] = []
        self.seen: set[tuple[int, int]] = set()

        # Track active frontiers: {source: (left, right)}
        self.active_frontiers = {}

        # For large inputs, skip educational content entirely
        if self.is_large_input:
            # Brief mode indicator
            mode_text = Text(
                f"Large input mode: {n} vertices - skipping explanations",
                font_size=20, color=YELLOW
            )
            mode_text.to_edge(UP)
            self.play(FadeIn(mode_text), run_time=0.5)
            self.wait(1)
            self.play(FadeOut(mode_text), run_time=0.3)
        else:
            # === EXPLANATION: Chebyshev transformation ===
            exp1 = self.show_explanation([
                "Goal: Find the farthest pair of polygon vertices (by Manhattan distance)",
                "that forms a valid axis-aligned rectangle fully inside the polygon.",
            ], wait_time=4.0)
            self.hide_explanation(exp1)

            exp2 = self.show_explanation([
                "Key insight: Manhattan distance decomposes via Chebyshev transformation.",
                "d((x₁,y₁), (x₂,y₂)) = |x₁-x₂| + |y₁-y₂| = max(|u₁-u₂|, |v₁-v₂|)",
                "where u = x+y and v = x-y (the Chebyshev coordinates).",
            ], wait_time=5.0)
            self.hide_explanation(exp2)

            exp3 = self.show_explanation([
                "This means: Manhattan distance = max of two independent 1D distances.",
                "So the farthest pair is either the u-extremes OR the v-extremes.",
                "Let's see why only extreme points matter...",
            ], wait_time=4.0)
            self.hide_explanation(exp3)

            # Detailed illustration of why only min/max combinations are optimal
            self.illustrate_vector_decomposition()

            exp3b = self.show_explanation([
                "Back to our problem: we track points sorted by u (orange) and v (purple).",
                "The same logic applies to both dimensions independently.",
            ], wait_time=4.0)
            self.hide_explanation(exp3b)

            # === EXPLANATION: Initial candidates ===
            exp4 = self.show_explanation([
                "We insert two initial candidates into a max-heap:",
                "• The pair with extreme u values (max u - min u)",
                "• The pair with extreme v values (max v - min v)",
                "The heap orders by Manhattan distance, so we always check the farthest pair first.",
            ], wait_time=5.0)
            self.hide_explanation(exp4)

        # Add initial candidates
        self._add_candidate(0, n - 1, 'u', self.by_u)
        self._add_candidate(0, n - 1, 'v', self.by_v)

        # Initial displays
        self.update_heap_display()
        self.update_all_frontier_displays()
        self.wait(1)

        # Run algorithm
        self.run_algorithm()

    def create_cartesian_panel(self):
        """Create the left panel with Cartesian grid and polygon."""
        # Title
        num_cells_x = len(self.xs) - 1
        num_cells_y = len(self.ys) - 1
        title = Text(f"Cartesian (x, y) - {num_cells_x}×{num_cells_y} grid", font_size=20, color=WHITE)
        title.move_to(LEFT_PANEL_CENTER + UP * 3)
        self.add(title)

        # For large grids, skip individual cell rendering (too slow and invisible)
        max_cells_to_render = 100 * 100  # 10,000 cells max
        show_grid = (num_cells_x * num_cells_y) <= max_cells_to_render

        self.grid_cells = VGroup()

        if show_grid:
            for i in range(num_cells_x):
                for j in range(num_cells_y):
                    cell = Rectangle(
                        width=self.cell_size,
                        height=self.cell_size,
                        fill_opacity=1,
                        stroke_width=0.5,
                        stroke_color=GRAY_D
                    )
                    cell.set_fill(COLOR_INSIDE if self.inside[i][j] else COLOR_OUTSIDE)
                    x = (i + 0.5) * self.cell_size
                    y = (j + 0.5) * self.cell_size
                    cell.move_to(self.grid_offset + np.array([x, y, 0]))
                    self.grid_cells.add(cell)
            self.add(self.grid_cells)
        else:
            # For large grids, just show a background rectangle
            grid_width = num_cells_x * self.cell_size
            grid_height = num_cells_y * self.cell_size
            bg = Rectangle(
                width=grid_width, height=grid_height,
                fill_color=COLOR_INSIDE, fill_opacity=0.3,
                stroke_color=GRAY_D, stroke_width=1
            )
            bg.move_to(self.grid_offset + np.array([grid_width/2, grid_height/2, 0]))
            self.add(bg)

        # Polygon edges and vertices
        self.polygon_group = VGroup()
        n = len(self.points)

        # Adjust sizes for large inputs
        edge_width = 1 if n > 50 else 3
        vertex_radius = 0.02 if n > 50 else 0.06

        for i in range(n):
            p1 = self.points[i]
            p2 = self.points[(i + 1) % n]
            start = self.cartesian_to_screen(p1)
            end = self.cartesian_to_screen(p2)
            edge = Line(start, end, color=COLOR_EDGE, stroke_width=edge_width)
            self.polygon_group.add(edge)

        if n <= LARGE_INPUT_THRESHOLD:
            for p in self.points:
                pos = self.cartesian_to_screen(p)
                vertex = Dot(pos, color=COLOR_VERTEX, radius=vertex_radius)
                self.polygon_group.add(vertex)

        self.add(self.polygon_group)

    def create_chebyshev_panel(self):
        """Create the right panel with Chebyshev space visualization."""
        # Title
        title = Text("Chebyshev (u, v)", font_size=20, color=WHITE)
        title.move_to(RIGHT_PANEL_CENTER + UP * 3)
        self.add(title)

        # Axes
        axis_length = 2.5
        u_axis = Arrow(
            RIGHT_PANEL_CENTER + LEFT * axis_length,
            RIGHT_PANEL_CENTER + RIGHT * axis_length,
            color=GRAY, stroke_width=1, buff=0
        )
        v_axis = Arrow(
            RIGHT_PANEL_CENTER + DOWN * axis_length,
            RIGHT_PANEL_CENTER + UP * axis_length,
            color=GRAY, stroke_width=1, buff=0
        )
        self.add(u_axis, v_axis)

        # Axis labels
        u_label = Text("u = x+y", font_size=14, color=GRAY)
        u_label.next_to(u_axis.get_end(), RIGHT, buff=0.1)
        v_label = Text("v = x-y", font_size=14, color=GRAY)
        v_label.next_to(v_axis.get_end(), UP, buff=0.1)
        self.add(u_label, v_label)

        # Plot points in Chebyshev space
        n = len(self.points)
        cheb_dot_radius = 0.02 if n > 50 else 0.06
        cheb_edge_width = 0.5 if n > 50 else 2

        self.cheb_dots = VGroup()
        if n <= LARGE_INPUT_THRESHOLD:
            for i, (u, v) in enumerate(self.cheb_points):
                pos = self.chebyshev_to_screen(u, v)
                dot = Dot(pos, color=COLOR_VERTEX, radius=cheb_dot_radius)
                self.cheb_dots.add(dot)
            self.add(self.cheb_dots)

            for i in range(n):
                u1, v1 = self.cheb_points[i]
                u2, v2 = self.cheb_points[(i + 1) % n]
                start = self.chebyshev_to_screen(u1, v1)
                end = self.chebyshev_to_screen(u2, v2)
                edge = Line(start, end, color=COLOR_EDGE, stroke_width=cheb_edge_width, stroke_opacity=0.5)
                self.add(edge)

    def create_frontier_displays(self):
        """Create the frontier number lines for by_u and by_v."""
        n = len(self.points)
        # For large inputs, skip individual dots (too cluttered)
        show_dots = n <= 50

        # U-frontier (horizontal, above Chebyshev panel)
        u_bar_y = RIGHT_PANEL_CENTER[1] + 2.5
        u_bar_width = 4.0
        u_bar_left = RIGHT_PANEL_CENTER[0] - u_bar_width / 2

        self.u_frontier_line = Line(
            np.array([u_bar_left, u_bar_y, 0]),
            np.array([u_bar_left + u_bar_width, u_bar_y, 0]),
            color=COLOR_U_FRONTIER, stroke_width=2
        )
        self.add(self.u_frontier_line)

        u_label = Text(f"by_u (n={n})", font_size=12, color=COLOR_U_FRONTIER)
        u_label.next_to(self.u_frontier_line, LEFT, buff=0.1)
        self.add(u_label)

        # Dots for each point sorted by u (skip for large inputs)
        self.u_dots = VGroup()
        if show_dots:
            for idx, point_idx in enumerate(self.by_u):
                x = u_bar_left + (idx + 0.5) * u_bar_width / n
                dot = Dot(np.array([x, u_bar_y, 0]), color=WHITE, radius=0.05)
                self.u_dots.add(dot)
            self.add(self.u_dots)

        # U-frontier bracket (will be updated)
        self.u_bracket = VGroup()
        self.add(self.u_bracket)

        # V-frontier (vertical, right of Chebyshev panel)
        v_bar_x = RIGHT_PANEL_CENTER[0] + 3.0
        v_bar_height = 4.0
        v_bar_bottom = RIGHT_PANEL_CENTER[1] - v_bar_height / 2

        self.v_frontier_line = Line(
            np.array([v_bar_x, v_bar_bottom, 0]),
            np.array([v_bar_x, v_bar_bottom + v_bar_height, 0]),
            color=COLOR_V_FRONTIER, stroke_width=2
        )
        self.add(self.v_frontier_line)

        v_label = Text(f"by_v (n={n})", font_size=12, color=COLOR_V_FRONTIER)
        v_label.next_to(self.v_frontier_line, RIGHT, buff=0.1)
        self.add(v_label)

        # Dots for each point sorted by v (skip for large inputs)
        self.v_dots = VGroup()
        if show_dots:
            for idx, point_idx in enumerate(self.by_v):
                y = v_bar_bottom + (idx + 0.5) * v_bar_height / n
                dot = Dot(np.array([v_bar_x, y, 0]), color=WHITE, radius=0.05)
                self.v_dots.add(dot)
            self.add(self.v_dots)

        # V-frontier bracket (will be updated)
        self.v_bracket = VGroup()
        self.add(self.v_bracket)

        # Store dimensions for later
        self.u_bar_left = u_bar_left
        self.u_bar_width = u_bar_width
        self.u_bar_y = u_bar_y
        self.v_bar_x = v_bar_x
        self.v_bar_bottom = v_bar_bottom
        self.v_bar_height = v_bar_height

        # Persistent constraint lines in Chebyshev space (always visible)
        # These show the extreme u and v values from current frontiers
        self.u_constraint_lines = VGroup()  # Vertical lines at u extremes
        self.v_constraint_lines = VGroup()  # Horizontal lines at v extremes
        self.add(self.u_constraint_lines)
        self.add(self.v_constraint_lines)

    def update_all_frontier_displays(self):
        """Update frontier bracket displays based on active frontiers."""
        n = len(self.points)

        # Clear old brackets
        self.u_bracket.submobjects.clear()
        self.v_bracket.submobjects.clear()

        # Find global extreme indices across ALL candidates in heap for brackets
        u_min_idx = None
        u_max_idx = None
        v_min_idx = None
        v_max_idx = None

        for cand in self.heap:
            if cand.source == 'u':
                if u_min_idx is None or cand.left < u_min_idx:
                    u_min_idx = cand.left
                if u_max_idx is None or cand.right > u_max_idx:
                    u_max_idx = cand.right
            else:
                if v_min_idx is None or cand.left < v_min_idx:
                    v_min_idx = cand.left
                if v_max_idx is None or cand.right > v_max_idx:
                    v_max_idx = cand.right

        # Draw U bracket spanning global u-range
        if u_min_idx is not None and u_max_idx is not None:
            x1 = self.u_bar_left + u_min_idx * self.u_bar_width / n
            x2 = self.u_bar_left + (u_max_idx + 1) * self.u_bar_width / n
            bracket = Rectangle(
                width=x2 - x1, height=0.15,
                color=COLOR_U_FRONTIER, fill_opacity=0.3, stroke_width=2
            )
            bracket.move_to(np.array([(x1 + x2) / 2, self.u_bar_y, 0]))
            self.u_bracket.add(bracket)

        # Draw V bracket spanning global v-range
        if v_min_idx is not None and v_max_idx is not None:
            y1 = self.v_bar_bottom + v_min_idx * self.v_bar_height / n
            y2 = self.v_bar_bottom + (v_max_idx + 1) * self.v_bar_height / n
            bracket = Rectangle(
                width=0.15, height=y2 - y1,
                color=COLOR_V_FRONTIER, fill_opacity=0.3, stroke_width=2
            )
            bracket.move_to(np.array([self.v_bar_x, (y1 + y2) / 2, 0]))
            self.v_bracket.add(bracket)

    def update_constraint_lines(self, u_vals: tuple[int, int], v_vals: tuple[int, int]):
        """Update the 4 constraint lines to pass through specified u and v values."""
        self.u_constraint_lines.submobjects.clear()
        self.v_constraint_lines.submobjects.clear()

        u1, u2 = u_vals
        v1, v2 = v_vals

        # Vertical lines at u = u1 and u = u2
        line_u1 = DashedLine(
            self.chebyshev_to_screen(u1, self.v_min - 20),
            self.chebyshev_to_screen(u1, self.v_max + 20),
            color=COLOR_U_FRONTIER, stroke_width=2, stroke_opacity=0.6
        )
        line_u2 = DashedLine(
            self.chebyshev_to_screen(u2, self.v_min - 20),
            self.chebyshev_to_screen(u2, self.v_max + 20),
            color=COLOR_U_FRONTIER, stroke_width=2, stroke_opacity=0.6
        )
        self.u_constraint_lines.add(line_u1, line_u2)

        # Horizontal lines at v = v1 and v = v2
        line_v1 = DashedLine(
            self.chebyshev_to_screen(self.u_min - 20, v1),
            self.chebyshev_to_screen(self.u_max + 20, v1),
            color=COLOR_V_FRONTIER, stroke_width=2, stroke_opacity=0.6
        )
        line_v2 = DashedLine(
            self.chebyshev_to_screen(self.u_min - 20, v2),
            self.chebyshev_to_screen(self.u_max + 20, v2),
            color=COLOR_V_FRONTIER, stroke_width=2, stroke_opacity=0.6
        )
        self.v_constraint_lines.add(line_v1, line_v2)

    def cartesian_to_screen(self, point: tuple[int, int]) -> np.ndarray:
        """Convert Cartesian data point to screen coordinates."""
        x, y = point
        xi = self.x_index[x]
        yi = self.y_index[y]
        screen_x = xi * self.cell_size
        screen_y = yi * self.cell_size
        return self.grid_offset + np.array([screen_x, screen_y, 0])

    def chebyshev_to_screen(self, u: int, v: int) -> np.ndarray:
        """Convert Chebyshev coordinates to screen position."""
        u_center = (self.u_min + self.u_max) / 2
        v_center = (self.v_min + self.v_max) / 2
        screen_x = (u - u_center) * self.cheb_scale
        screen_y = (v - v_center) * self.cheb_scale
        return RIGHT_PANEL_CENTER + np.array([screen_x, screen_y, 0])

    def create_heap_background(self) -> VGroup:
        """Create semi-transparent background for heap display."""
        bg = Rectangle(
            width=6, height=1.8,
            fill_color=COLOR_HEAP_BG, fill_opacity=0.85,
            stroke_color=WHITE, stroke_width=1
        )
        bg.move_to(HEAP_CENTER)
        title = Text("Candidate Heap (max by distance)", font_size=14, color=WHITE)
        title.next_to(bg.get_top(), DOWN, buff=0.1)
        return VGroup(bg, title)

    def update_heap_display(self):
        """Update heap tree visualization."""
        self.heap_group.submobjects.clear()

        if not self.heap:
            return

        nodes_to_show = min(len(self.heap), MAX_HEAP_NODES)
        node_radius = 0.15
        level_height = 0.45
        base_pos = HEAP_CENTER + UP * 0.5

        node_positions = []
        node_objects = []

        for idx in range(nodes_to_show):
            level = 0
            temp = idx + 1
            while temp > 1:
                temp //= 2
                level += 1

            level_start = (1 << level) - 1
            pos_in_level = idx - level_start
            level_count = 1 << level

            h_spread = 2.5 / (level + 1)
            x_offset = (pos_in_level - (level_count - 1) / 2) * h_spread
            y_offset = -level * level_height

            pos = base_pos + np.array([x_offset, y_offset, 0])
            node_positions.append(pos)

            candidate = self.heap[idx]
            # Color by source dimension
            fill_color = COLOR_U_FRONTIER if candidate.source == 'u' else COLOR_V_FRONTIER
            if idx == 0:
                fill_color = YELLOW

            circle = Circle(radius=node_radius, color=WHITE, fill_opacity=0.9, fill_color=fill_color)
            circle.move_to(pos)

            label = Text(str(candidate.distance), font_size=10, color=WHITE)
            label.move_to(pos)

            node_objects.append(VGroup(circle, label))

        for idx in range(1, nodes_to_show):
            parent_idx = (idx - 1) // 2
            if parent_idx < len(node_positions):
                edge = Line(node_positions[parent_idx], node_positions[idx], stroke_width=1, color=GRAY)
                self.heap_group.add(edge)

        for node in node_objects:
            self.heap_group.add(node)

    def _add_candidate(self, left: int, right: int, source: str, sorted_arr: list[int]):
        """Add candidate to heap if valid."""
        if left < right:
            i = sorted_arr[left]
            j = sorted_arr[right]
            pair = (min(i, j), max(i, j))
            if pair not in self.seen:
                dist = manhattan_distance(self.points[i], self.points[j])
                heapq.heappush(self.heap, Candidate(dist, pair[0], pair[1], source, left, right))

    def is_valid_rectangle(self, i: int, j: int) -> bool:
        """Check if pair forms valid rectangle."""
        x1, y1 = self.points[i]
        x2, y2 = self.points[j]

        if x1 == x2 or y1 == y2:
            return False

        min_x, max_x = min(x1, x2), max(x1, x2)
        min_y, max_y = min(y1, y2), max(y1, y2)

        i1 = self.x_index[min_x]
        i2 = self.x_index[max_x]
        j1 = self.y_index[min_y]
        j2 = self.y_index[max_y]

        total_cells = (i2 - i1) * (j2 - j1)
        inside_cells = count_inside(i1, j1, i2, j2, self.prefix)

        return inside_cells == total_cells

    def precompute_animation_count(self) -> tuple[int, int]:
        """
        Dry-run the algorithm to count total candidates and estimate animations.
        Returns (total_candidates, estimated_animations).
        """
        # Copy initial state
        heap: list[Candidate] = []
        seen: set[tuple[int, int]] = set()
        n = len(self.points)

        # Add initial candidates (same as real algorithm)
        def add_candidate(left: int, right: int, source: str, sorted_arr: list[int]):
            if left < right:
                i = sorted_arr[left]
                j = sorted_arr[right]
                pair = (min(i, j), max(i, j))
                if pair not in seen:
                    dist = manhattan_distance(self.points[i], self.points[j])
                    heapq.heappush(heap, Candidate(dist, pair[0], pair[1], source, left, right))

        add_candidate(0, n - 1, 'u', self.by_u)
        add_candidate(0, n - 1, 'v', self.by_v)

        total_candidates = 0
        animated_candidates = 0

        while heap:
            candidate = heapq.heappop(heap)
            pair = (candidate.i, candidate.j)

            if pair in seen:
                continue
            seen.add(pair)
            total_candidates += 1

            # Count animations: intro phase animates all, fast phase animates every Nth
            if total_candidates <= INTRO_ITERATIONS:
                animated_candidates += 1
            elif total_candidates % ANIMATE_EVERY_N_FAST == 0:
                animated_candidates += 1

            # Check if valid (would terminate)
            if self.is_valid_rectangle(candidate.i, candidate.j):
                break

            # Expand frontier
            sorted_arr = self.by_u if candidate.source == 'u' else self.by_v
            add_candidate(candidate.left + 1, candidate.right, candidate.source, sorted_arr)
            add_candidate(candidate.left, candidate.right - 1, candidate.source, sorted_arr)

        # Estimate total animations:
        # - Intro explanations: ~15 play() calls
        # - Vector decomposition: ~30 play() calls
        # - Per animated candidate: ~8 play() calls (check + expand)
        estimated_animations = 45 + (animated_candidates * 8)

        return total_candidates, estimated_animations

    def run_algorithm(self):
        """Run the main algorithm with animation."""
        # Pre-compute totals and log
        self.total_candidates, self.estimated_animations = self.precompute_animation_count()

        # Determine animation settings based on input size
        if self.is_large_input:
            animate_every_n = LARGE_ANIMATE_EVERY_N
            max_animated = MAX_ANIMATED_CANDIDATES
            anim_speed = LARGE_ANIMATION_SPEED
            estimated_animated = min(max_animated, self.total_candidates // animate_every_n + 1)
        else:
            animate_every_n = ANIMATE_EVERY_N_FAST
            max_animated = float('inf')  # No cap for small inputs
            anim_speed = FAST_ANIMATION_SPEED
            estimated_animated = max(0, (self.total_candidates - INTRO_ITERATIONS) // animate_every_n)

        print("\n" + "=" * 60)
        print("ANIMATION ESTIMATE (pre-render)")
        print(f"  Total candidates to check: {self.total_candidates}")
        print(f"  Large input mode: {self.is_large_input}")
        print(f"  Estimated animated candidates: ~{estimated_animated}")
        print("=" * 60 + "\n")

        logger.info("=" * 60)
        logger.info(f"ANIMATION ESTIMATE")
        logger.info(f"  Total candidates to check: {self.total_candidates}")
        logger.info(f"  Large input mode: {self.is_large_input}")
        logger.info(f"  Estimated animated candidates: ~{estimated_animated}")
        logger.info("=" * 60)

        first_iteration = True
        first_expansion = True
        iteration_count = 0
        self.candidates_considered = 0
        self.fast_mode = self.is_large_input  # Start in fast mode for large inputs
        self.progress_text = None
        self.animated_count = 0

        # Create stats display for large inputs
        if self.is_large_input:
            self.create_stats_display()

        while self.heap:
            candidate = heapq.heappop(self.heap)
            pair = (candidate.i, candidate.j)

            if pair in self.seen:
                continue
            self.seen.add(pair)
            iteration_count += 1
            self.candidates_considered += 1

            # For small inputs: switch to fast mode after intro iterations
            if not self.is_large_input and not self.fast_mode and iteration_count > INTRO_ITERATIONS:
                self.fast_mode = True
                exp = self.show_explanation([
                    f"Algorithm demonstrated! Switching to fast mode...",
                    f"Will show every {animate_every_n}th candidate.",
                ], wait_time=2.0)
                self.hide_explanation(exp)

            # Update stats display for large inputs (every iteration for accurate tracking)
            if self.is_large_input:
                # Update stats display frequently but not every single iteration to save render time
                if iteration_count % 50 == 0 or iteration_count <= 5:
                    self.update_stats_display(candidate.distance)
            elif self.fast_mode and iteration_count % SHOW_PROGRESS_EVERY == 0:
                self.update_progress(iteration_count, candidate.distance)

            # Determine whether to animate this iteration
            if self.is_large_input:
                # Large input mode: animate every Nth candidate, up to max
                should_animate = (
                    iteration_count % animate_every_n == 0 and
                    self.animated_count < max_animated
                )
            else:
                # Small input mode: original logic
                should_animate = (not self.fast_mode) or (iteration_count % animate_every_n == 0)

            # For small inputs: Explain what we're checking on first iteration
            if first_iteration and not self.is_large_input:
                first_iteration = False
                exp = self.show_explanation([
                    "Pop the max-distance candidate from the heap.",
                    "Check if its two corners form a valid rectangle:",
                    "all grid cells inside the rectangle must be inside the polygon.",
                ], wait_time=4.0)
                self.hide_explanation(exp)

            if should_animate:
                self.animated_count += 1
                if self.is_large_input:
                    self.animate_candidate_check_fast(candidate)
                else:
                    self.animate_candidate_check(candidate)

            if self.is_valid_rectangle(candidate.i, candidate.j):
                # Final stats update before success
                if self.is_large_input:
                    self.update_stats_display(candidate.distance)
                self.animate_success(candidate)
                return

            # For small inputs: Explain frontier expansion on first invalid candidate
            if first_expansion and should_animate and not self.is_large_input:
                first_expansion = False
                exp = self.show_explanation([
                    "Rectangle invalid! We must try smaller distances.",
                    "Key insight: max(u₁-u₂) = max(u) - min(u) for points in a range.",
                    "The next-best distance must give up either the max or the min.",
                ], wait_time=5.0)
                self.hide_explanation(exp)

                # Highlight the two extreme points from this candidate's source dimension
                source_dim = "u" if candidate.source == 'u' else "v"
                source_color = COLOR_U_FRONTIER if candidate.source == 'u' else COLOR_V_FRONTIER
                sorted_arr = self.by_u if candidate.source == 'u' else self.by_v
                left_idx = sorted_arr[candidate.left]
                right_idx = sorted_arr[candidate.right]

                # Create highlights in Chebyshev space
                left_u, left_v = self.cheb_points[left_idx]
                right_u, right_v = self.cheb_points[right_idx]
                left_dot = Dot(self.chebyshev_to_screen(left_u, left_v), color=source_color, radius=0.15)
                right_dot = Dot(self.chebyshev_to_screen(right_u, right_v), color=source_color, radius=0.15)
                extreme_line = Line(
                    self.chebyshev_to_screen(left_u, left_v),
                    self.chebyshev_to_screen(right_u, right_v),
                    color=source_color, stroke_width=4
                )

                self.play(FadeIn(left_dot), FadeIn(right_dot), Create(extreme_line), run_time=0.5)

                exp2 = self.show_explanation([
                    f"This candidate came from the {source_dim}-sorted list (shown highlighted).",
                    f"It was maximal in the {source_dim}-direction, so we relax along {source_dim}.",
                    "We spawn two children by shrinking the range:",
                    "• (left+1, right): give up the min-" + source_dim + " extreme",
                    "• (left, right-1): give up the max-" + source_dim + " extreme",
                ], wait_time=6.0)
                self.hide_explanation(exp2)

                self.play(FadeOut(left_dot), FadeOut(right_dot), FadeOut(extreme_line), run_time=0.3)

            # Add new candidates
            if should_animate and not self.is_large_input:
                # Full animation for small inputs
                self.animate_frontier_expansion(candidate)
            else:
                # Fast path: just add candidates without animation
                sorted_arr = self.by_u if candidate.source == 'u' else self.by_v
                self._add_candidate(candidate.left + 1, candidate.right, candidate.source, sorted_arr)
                self._add_candidate(candidate.left, candidate.right - 1, candidate.source, sorted_arr)

        # Final stats update
        if self.is_large_input:
            self.update_stats_display(0)
        self.show_no_result()

    def animate_frontier_expansion(self, candidate: Candidate):
        """Animate adding two new candidates, showing endpoint selection clearly."""
        sorted_arr = self.by_u if candidate.source == 'u' else self.by_v
        n = len(self.points)
        source_color = COLOR_U_FRONTIER if candidate.source == 'u' else COLOR_V_FRONTIER
        anim_speed = self.get_animation_speed()
        pause_dur = self.get_pause_duration()

        left, right = candidate.left, candidate.right

        # Get the endpoint indices and positions
        left_idx = sorted_arr[left]
        right_idx = sorted_arr[right]

        # Screen positions for endpoints on the frontier bar
        if candidate.source == 'u':
            left_pos = np.array([self.u_bar_left + (left + 0.5) * self.u_bar_width / n, self.u_bar_y, 0])
            right_pos = np.array([self.u_bar_left + (right + 0.5) * self.u_bar_width / n, self.u_bar_y, 0])
        else:
            left_pos = np.array([self.v_bar_x, self.v_bar_bottom + (left + 0.5) * self.v_bar_height / n, 0])
            right_pos = np.array([self.v_bar_x, self.v_bar_bottom + (right + 0.5) * self.v_bar_height / n, 0])

        # Chebyshev positions and coordinates
        left_u, left_v = self.cheb_points[left_idx]
        right_u, right_v = self.cheb_points[right_idx]
        left_cheb = self.chebyshev_to_screen(left_u, left_v)
        right_cheb = self.chebyshev_to_screen(right_u, right_v)

        # Create highlight dots for both endpoints
        left_highlight = Dot(left_pos, color=COLOR_CANDIDATE, radius=0.12)
        right_highlight = Dot(right_pos, color=COLOR_CANDIDATE, radius=0.12)
        left_cheb_highlight = Dot(left_cheb, color=COLOR_CANDIDATE, radius=0.1)
        right_cheb_highlight = Dot(right_cheb, color=COLOR_CANDIDATE, radius=0.1)

        # Step 1: Highlight both endpoints (left, right) - constraint lines already set from candidate check
        self.play(
            FadeIn(left_highlight), FadeIn(right_highlight),
            FadeIn(left_cheb_highlight), FadeIn(right_cheb_highlight),
            run_time=anim_speed * 0.4
        )

        self.maybe_wait(pause_dur)  # Pause to show current range

        persistent_lines = VGroup()

        # Step 2: First new candidate - give up left extreme (left+1, right)
        if left + 1 < right:
            new_left_idx = sorted_arr[left + 1]
            new_left_u, new_left_v = self.cheb_points[new_left_idx]
            if candidate.source == 'u':
                new_left_pos = np.array([self.u_bar_left + (left + 1 + 0.5) * self.u_bar_width / n, self.u_bar_y, 0])
            else:
                new_left_pos = np.array([self.v_bar_x, self.v_bar_bottom + (left + 1 + 0.5) * self.v_bar_height / n, 0])
            new_left_cheb = self.chebyshev_to_screen(new_left_u, new_left_v)

            # Update constraint lines to pass through the NEW candidate's two points
            # New candidate is (new_left_idx, right_idx)
            self.update_constraint_lines(
                (min(new_left_u, right_u), max(new_left_u, right_u)),
                (min(new_left_v, right_v), max(new_left_v, right_v))
            )

            # Animate left highlight moving inward
            self.play(
                left_highlight.animate.move_to(new_left_pos).set_color(source_color),
                left_cheb_highlight.animate.move_to(new_left_cheb).set_color(source_color),
                run_time=anim_speed * 0.5
            )

            # Draw line for this new candidate
            new_line1 = Line(new_left_cheb, right_cheb, color=source_color, stroke_width=2)
            self.play(Create(new_line1), run_time=anim_speed * 0.3)
            persistent_lines.add(new_line1)

            self.maybe_wait(pause_dur)  # Pause to show first new candidate

            # Add to heap
            self._add_candidate(left + 1, right, candidate.source, sorted_arr)

            # Reset constraint lines back to original candidate's points
            self.update_constraint_lines(
                (min(left_u, right_u), max(left_u, right_u)),
                (min(left_v, right_v), max(left_v, right_v))
            )

            # Reset left highlight back to original position
            self.play(
                left_highlight.animate.move_to(left_pos).set_color(COLOR_CANDIDATE),
                left_cheb_highlight.animate.move_to(left_cheb).set_color(COLOR_CANDIDATE),
                run_time=anim_speed * 0.4
            )

            self.maybe_wait(pause_dur / 2)  # Brief pause before second candidate

        # Step 3: Second new candidate - give up right extreme (left, right-1)
        if left < right - 1:
            new_right_idx = sorted_arr[right - 1]
            new_right_u, new_right_v = self.cheb_points[new_right_idx]
            if candidate.source == 'u':
                new_right_pos = np.array([self.u_bar_left + (right - 1 + 0.5) * self.u_bar_width / n, self.u_bar_y, 0])
            else:
                new_right_pos = np.array([self.v_bar_x, self.v_bar_bottom + (right - 1 + 0.5) * self.v_bar_height / n, 0])
            new_right_cheb = self.chebyshev_to_screen(new_right_u, new_right_v)

            # Update constraint lines to pass through the NEW candidate's two points
            # New candidate is (left_idx, new_right_idx)
            self.update_constraint_lines(
                (min(left_u, new_right_u), max(left_u, new_right_u)),
                (min(left_v, new_right_v), max(left_v, new_right_v))
            )

            # Animate right highlight moving inward
            self.play(
                right_highlight.animate.move_to(new_right_pos).set_color(source_color),
                right_cheb_highlight.animate.move_to(new_right_cheb).set_color(source_color),
                run_time=anim_speed * 0.5
            )

            # Draw line for this new candidate
            new_line2 = Line(left_cheb, new_right_cheb, color=source_color, stroke_width=2)
            self.play(Create(new_line2), run_time=anim_speed * 0.3)
            persistent_lines.add(new_line2)

            self.maybe_wait(pause_dur)  # Pause to show second new candidate

            # Add to heap
            self._add_candidate(left, right - 1, candidate.source, sorted_arr)

        # Update displays
        self.update_heap_display()
        self.update_all_frontier_displays()

        self.maybe_wait(pause_dur / 2)  # Pause to show updated heap

        # Fade out all highlights and lines
        self.play(
            FadeOut(left_highlight), FadeOut(right_highlight),
            FadeOut(left_cheb_highlight), FadeOut(right_cheb_highlight),
            *[FadeOut(l) for l in persistent_lines],
            run_time=anim_speed * 0.4
        )

        self.maybe_wait(pause_dur / 2)  # Pause before next iteration

    def animate_candidate_check(self, candidate: Candidate):
        """Animate checking a candidate pair in both panels."""
        i, j = candidate.i, candidate.j
        p1 = self.points[i]
        p2 = self.points[j]

        # Cartesian panel: corners and rectangle
        corner1_cart = Dot(self.cartesian_to_screen(p1), color=COLOR_CANDIDATE, radius=0.12)
        corner2_cart = Dot(self.cartesian_to_screen(p2), color=COLOR_CANDIDATE, radius=0.12)

        # Chebyshev panel: corners and connection line
        u1, v1 = self.cheb_points[i]
        u2, v2 = self.cheb_points[j]
        corner1_cheb = Dot(self.chebyshev_to_screen(u1, v1), color=COLOR_CANDIDATE, radius=0.1)
        corner2_cheb = Dot(self.chebyshev_to_screen(u2, v2), color=COLOR_CANDIDATE, radius=0.1)
        cheb_line = DashedLine(
            self.chebyshev_to_screen(u1, v1),
            self.chebyshev_to_screen(u2, v2),
            color=COLOR_CANDIDATE, stroke_width=3
        )

        # Update constraint lines to pass through the two candidate points
        # Each point intersects either a u or v frontier line
        self.update_constraint_lines((min(u1, u2), max(u1, u2)), (min(v1, v2), max(v1, v2)))

        # Highlight frontier range on the bar
        n = len(self.points)
        frontier_highlight = None
        if candidate.source == 'u':
            x1 = self.u_bar_left + candidate.left * self.u_bar_width / n
            x2 = self.u_bar_left + (candidate.right + 1) * self.u_bar_width / n
            frontier_highlight = Rectangle(
                width=x2 - x1, height=0.25,
                color=COLOR_CANDIDATE, fill_opacity=0.5, stroke_width=3
            )
            frontier_highlight.move_to(np.array([(x1 + x2) / 2, self.u_bar_y, 0]))
        else:
            y1 = self.v_bar_bottom + candidate.left * self.v_bar_height / n
            y2 = self.v_bar_bottom + (candidate.right + 1) * self.v_bar_height / n
            frontier_highlight = Rectangle(
                width=0.25, height=y2 - y1,
                color=COLOR_CANDIDATE, fill_opacity=0.5, stroke_width=3
            )
            frontier_highlight.move_to(np.array([self.v_bar_x, (y1 + y2) / 2, 0]))

        x1, y1 = p1
        x2, y2 = p2

        if x1 != x2 and y1 != y2:
            min_x, max_x = min(x1, x2), max(x1, x2)
            min_y, max_y = min(y1, y2), max(y1, y2)

            bl = self.cartesian_to_screen((min_x, min_y))
            tr = self.cartesian_to_screen((max_x, max_y))

            rect = Rectangle(
                width=tr[0] - bl[0], height=tr[1] - bl[1],
                stroke_color=COLOR_CANDIDATE, stroke_width=4, fill_opacity=0
            )
            rect.move_to((bl + tr) / 2)

            # Animate the rectangle and connection with constraint lines visible
            anim_speed = self.get_animation_speed()
            pause_dur = self.get_pause_duration()

            self.play(
                Create(rect),
                Create(cheb_line),
                FadeIn(corner1_cart), FadeIn(corner2_cart),
                FadeIn(corner1_cheb), FadeIn(corner2_cheb),
                FadeIn(frontier_highlight),
                run_time=anim_speed
            )

            self.maybe_wait(pause_dur)  # Pause to view candidate

            is_valid = self.is_valid_rectangle(i, j)
            flash_color = COLOR_VALID if is_valid else COLOR_INVALID

            self.play(
                rect.animate.set_stroke(color=flash_color),
                cheb_line.animate.set_color(flash_color),
                run_time=anim_speed / 2
            )

            self.maybe_wait(pause_dur)  # Pause to view result

            self.play(
                FadeOut(rect), FadeOut(cheb_line),
                FadeOut(corner1_cart), FadeOut(corner2_cart),
                FadeOut(corner1_cheb), FadeOut(corner2_cheb),
                FadeOut(frontier_highlight),
                run_time=anim_speed / 2
            )

            self.maybe_wait(pause_dur / 2)  # Brief pause before next step
        else:
            anim_speed = self.get_animation_speed()
            pause_dur = self.get_pause_duration()

            self.play(
                Create(cheb_line),
                FadeIn(corner1_cart), FadeIn(corner2_cart),
                FadeIn(corner1_cheb), FadeIn(corner2_cheb),
                FadeIn(frontier_highlight),
                run_time=anim_speed / 2
            )

            self.maybe_wait(pause_dur / 2)  # Brief pause

            self.play(
                FadeOut(cheb_line),
                FadeOut(corner1_cart), FadeOut(corner2_cart),
                FadeOut(corner1_cheb), FadeOut(corner2_cheb),
                FadeOut(frontier_highlight),
                run_time=anim_speed / 2
            )

            self.maybe_wait(pause_dur / 2)  # Brief pause before next step

    def animate_candidate_check_fast(self, candidate: Candidate):
        """Simplified animation for large inputs - rectangle flash only."""
        i, j = candidate.i, candidate.j
        p1 = self.points[i]
        p2 = self.points[j]

        x1, y1 = p1
        x2, y2 = p2

        if x1 != x2 and y1 != y2:
            min_x, max_x = min(x1, x2), max(x1, x2)
            min_y, max_y = min(y1, y2), max(y1, y2)

            bl = self.cartesian_to_screen((min_x, min_y))
            tr = self.cartesian_to_screen((max_x, max_y))

            is_valid = self.is_valid_rectangle(i, j)
            flash_color = COLOR_VALID if is_valid else COLOR_INVALID

            rect = Rectangle(
                width=tr[0] - bl[0], height=tr[1] - bl[1],
                stroke_color=flash_color, stroke_width=4, fill_opacity=0
            )
            rect.move_to((bl + tr) / 2)

            # Single quick animation: show and remove
            self.play(Create(rect), run_time=LARGE_ANIMATION_SPEED)
            self.play(FadeOut(rect), run_time=LARGE_ANIMATION_SPEED)

    def animate_success(self, candidate: Candidate):
        """Celebrate finding valid rectangle."""
        # Remove progress/stats displays
        if self.progress_text:
            self.remove(self.progress_text)
            self.progress_text = None
        if self.is_large_input and hasattr(self, 'stats_bg'):
            self.remove(self.stats_bg)
            if self.stats_text:
                self.remove(self.stats_text)

        # Show success explanation (skip for large inputs)
        if not self.is_large_input:
            exp = self.show_explanation([
                "Found it! This rectangle is fully inside the polygon.",
                "Since we process by decreasing Manhattan distance,",
                "this is guaranteed to be the largest valid rectangle.",
            ], wait_time=4.0)
            self.hide_explanation(exp)

        i, j = candidate.i, candidate.j
        p1 = self.points[i]
        p2 = self.points[j]

        x1, y1 = p1
        x2, y2 = p2
        min_x, max_x = min(x1, x2), max(x1, x2)
        min_y, max_y = min(y1, y2), max(y1, y2)

        bl = self.cartesian_to_screen((min_x, min_y))
        tr = self.cartesian_to_screen((max_x, max_y))

        rect = Rectangle(
            width=tr[0] - bl[0], height=tr[1] - bl[1],
            stroke_color=COLOR_VALID, stroke_width=6,
            fill_color=COLOR_VALID, fill_opacity=0.3
        )
        rect.move_to((bl + tr) / 2)

        # Chebyshev success line (skip for large inputs)
        cheb_line = None
        if not self.is_large_input:
            u1, v1 = self.cheb_points[i]
            u2, v2 = self.cheb_points[j]
            cheb_line = Line(
                self.chebyshev_to_screen(u1, v1),
                self.chebyshev_to_screen(u2, v2),
                color=COLOR_VALID, stroke_width=4
            )

        area = (abs(x2 - x1) + 1) * (abs(y2 - y1) + 1)
        result_text = Text(
            f"Area: {area:,}  |  Candidates checked: {self.candidates_considered:,}",
            font_size=24, color=COLOR_VALID
        )
        result_text.to_edge(UP)

        if cheb_line:
            self.play(Create(rect), Create(cheb_line), Write(result_text), run_time=1)
        else:
            self.play(Create(rect), Write(result_text), run_time=1)
        self.wait(2)

    def show_no_result(self):
        """Show message when no valid rectangle found."""
        # Remove progress/stats displays
        if self.progress_text:
            self.remove(self.progress_text)
            self.progress_text = None
        if self.is_large_input and hasattr(self, 'stats_bg'):
            self.remove(self.stats_bg)
            if self.stats_text:
                self.remove(self.stats_text)

        if not self.is_large_input:
            exp = self.show_explanation([
                "Heap exhausted: no valid rectangle exists.",
                "The polygon has no pair of vertices that forms",
                "an axis-aligned rectangle fully inside the boundary.",
            ], wait_time=4.0)
            self.hide_explanation(exp)

        text = Text(
            f"No valid rectangle found  |  Candidates checked: {self.candidates_considered:,}",
            font_size=24, color=COLOR_INVALID
        )
        text.to_edge(UP)
        self.play(Write(text), run_time=1)
        self.wait(2)
