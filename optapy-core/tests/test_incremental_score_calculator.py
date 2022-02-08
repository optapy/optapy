import optapy
import optapy.score
import optapy.config
import optapy.constraint


@optapy.planning_entity
class Queen:
    def __init__(self, code, column, row=None):
        self.code = code
        self.row = row
        self.column = column

    @optapy.planning_variable(int, value_range_provider_refs=['row_range'])
    def get_row(self):
        return self.row

    def set_row(self, row):
        self.row = row

    def getColumnIndex(self):
        return self.column

    def getRowIndex(self):
        if self.row is None:
            return float('-inf')
        return self.row

    def getAscendingDiagonalIndex(self):
        return self.getColumnIndex() + self.getRowIndex()

    def getDescendingDiagonalIndex(self):
        return self.getColumnIndex() - self.getRowIndex()

    def __eq__(self, other):
        return self.code == other.code

    def __hash__(self):
        return hash(self.code)


@optapy.planning_solution
class Solution:
    def __init__(self, n, queen_list, column_list, row_list, score=None):
        self.n = n
        self.queen_list = queen_list
        self.column_list = column_list
        self.row_list = row_list
        self.score = score

    @optapy.planning_entity_collection_property(Queen)
    def get_queen_list(self):
        return self.queen_list

    @optapy.problem_fact_collection_property(int)
    @optapy.value_range_provider(range_id='row_range')
    def get_row_range(self):
        return self.row_list

    @optapy.planning_score(optapy.score.SimpleScore)
    def get_score(self) -> optapy.score.SimpleScore:
        return self.score

    def set_score(self, score):
        self.score = score


def test_constraint_match_disabled_incremental_score_calculator():
    @optapy.incremental_score_calculator
    class IncrementalScoreCalculator:
        score: int
        row_index_map: dict
        ascending_diagonal_index_map: dict
        descending_diagonal_index_map: dict

        def resetWorkingSolution(self, working_solution: Solution):
            n = working_solution.n
            self.row_index_map = dict()
            self.ascending_diagonal_index_map = dict()
            self.descending_diagonal_index_map = dict()
            for i in range(n):
                self.row_index_map[i] = list()
                self.ascending_diagonal_index_map[i] = list()
                self.descending_diagonal_index_map[i] = list()
                if i != 0:
                    self.ascending_diagonal_index_map[n - 1 + i] = list()
                    self.descending_diagonal_index_map[-i] = list()
            self.score = 0
            for queen in working_solution.queen_list:
                self.insert(queen)

        def beforeEntityAdded(self, entity: any):
            pass

        def afterEntityAdded(self, entity: any):
            self.insert(entity)

        def beforeVariableChanged(self, entity: any, variableName: str):
            self.retract(entity)

        def afterVariableChanged(self, entity: any, variableName: str):
            self.insert(entity)

        def beforeEntityRemoved(self, entity: any):
            self.retract(entity)

        def afterEntityRemoved(self, entity: any):
            pass

        def insert(self, queen: Queen):
            row = queen.row
            if row is not None:
                row_index = queen.row
                row_index_list = self.row_index_map[row_index]
                self.score -= len(row_index_list)
                row_index_list.append(queen)
                ascending_diagonal_index_list = self.ascending_diagonal_index_map[queen.getAscendingDiagonalIndex()]
                self.score -= len(ascending_diagonal_index_list)
                ascending_diagonal_index_list.append(queen)
                descending_diagonal_index_list = self.descending_diagonal_index_map[queen.getDescendingDiagonalIndex()]
                self.score -= len(descending_diagonal_index_list)
                descending_diagonal_index_list.append(queen)

        def retract(self, queen: Queen):
            row = queen.row
            if row is not None:
                row_index = queen.row
                row_index_list = self.row_index_map[row_index]
                row_index_list.remove(queen)
                self.score += len(row_index_list)
                ascending_diagonal_index_list = self.ascending_diagonal_index_map[queen.getAscendingDiagonalIndex()]
                ascending_diagonal_index_list.remove(queen)
                self.score += len(ascending_diagonal_index_list)
                descending_diagonal_index_list = self.descending_diagonal_index_map[queen.getDescendingDiagonalIndex()]
                descending_diagonal_index_list.remove(queen)
                self.score += len(descending_diagonal_index_list)

        def calculateScore(self) -> optapy.score.SimpleScore:
            return optapy.score.SimpleScore.of(self.score)

    solver_config = optapy.config.solver.SolverConfig()
    termination_config = optapy.config.solver.termination.TerminationConfig()
    termination_config.setBestScoreLimit('0')
    solver_config.withSolutionClass(optapy.get_class(Solution)) \
        .withEntityClasses(optapy.get_class(Queen)) \
        .withScoreDirectorFactory(optapy.config.score.director.ScoreDirectorFactoryConfig() \
                                  .withIncrementalScoreCalculatorClass(optapy.get_class(IncrementalScoreCalculator))) \
        .withTerminationConfig(termination_config)
    problem: Solution = Solution(4,
                                 [Queen('A', 0), Queen('B', 1), Queen('C', 2), Queen('D', 3)],
                                 [0, 1, 2, 3],
                                 [0, 1, 2, 3])
    solver = optapy.solver_factory_create(solver_config).buildSolver()
    solution = solver.solve(problem)
    assert solution.get_score().getScore() == 0
    for i in range(4):
        for j in range(i + 1, 4):
            left_queen = solution.queen_list[i]
            right_queen = solution.queen_list[j]
            assert left_queen.row is not None and right_queen.row is not None
            assert left_queen.row != right_queen.row
            assert left_queen.getAscendingDiagonalIndex() != right_queen.getAscendingDiagonalIndex()
            assert left_queen.getDescendingDiagonalIndex() != right_queen.getDescendingDiagonalIndex()


def test_constraint_match_enabled_incremental_score_calculator():
    @optapy.incremental_score_calculator
    class IncrementalScoreCalculator:
        score: int
        row_index_map: dict
        ascending_diagonal_index_map: dict
        descending_diagonal_index_map: dict

        def resetWorkingSolution(self, working_solution: Solution, constraint_match_enabled=False):
            n = working_solution.n
            self.row_index_map = dict()
            self.ascending_diagonal_index_map = dict()
            self.descending_diagonal_index_map = dict()
            for i in range(n):
                self.row_index_map[i] = list()
                self.ascending_diagonal_index_map[i] = list()
                self.descending_diagonal_index_map[i] = list()
                if i != 0:
                    self.ascending_diagonal_index_map[n - 1 + i] = list()
                    self.descending_diagonal_index_map[-i] = list()
            self.score = 0
            for queen in working_solution.queen_list:
                self.insert(queen)

        def beforeEntityAdded(self, entity: any):
            pass

        def afterEntityAdded(self, entity: any):
            self.insert(entity)

        def beforeVariableChanged(self, entity: any, variableName: str):
            self.retract(entity)

        def afterVariableChanged(self, entity: any, variableName: str):
            self.insert(entity)

        def beforeEntityRemoved(self, entity: any):
            self.retract(entity)

        def afterEntityRemoved(self, entity: any):
            pass

        def insert(self, queen: Queen):
            row = queen.row
            if row is not None:
                row_index = queen.row
                row_index_list = self.row_index_map[row_index]
                self.score -= len(row_index_list)
                row_index_list.append(queen)
                ascending_diagonal_index_list = self.ascending_diagonal_index_map[queen.getAscendingDiagonalIndex()]
                self.score -= len(ascending_diagonal_index_list)
                ascending_diagonal_index_list.append(queen)
                descending_diagonal_index_list = self.descending_diagonal_index_map[queen.getDescendingDiagonalIndex()]
                self.score -= len(descending_diagonal_index_list)
                descending_diagonal_index_list.append(queen)

        def retract(self, queen: Queen):
            row = queen.row
            if row is not None:
                row_index = queen.row
                row_index_list = self.row_index_map[row_index]
                row_index_list.remove(queen)
                self.score += len(row_index_list)
                ascending_diagonal_index_list = self.ascending_diagonal_index_map[queen.getAscendingDiagonalIndex()]
                ascending_diagonal_index_list.remove(queen)
                self.score += len(ascending_diagonal_index_list)
                descending_diagonal_index_list = self.descending_diagonal_index_map[queen.getDescendingDiagonalIndex()]
                descending_diagonal_index_list.remove(queen)
                self.score += len(descending_diagonal_index_list)

        def calculateScore(self) -> optapy.score.SimpleScore:
            return optapy.score.SimpleScore.of(self.score)

        def getConstraintMatchTotals(self):
            row_conflict_constraint_match_total = optapy.constraint.DefaultConstraintMatchTotal(
                'NQueens',
                'Row Conflict',
                optapy.score.SimpleScore.ONE)
            ascending_diagonal_constraint_match_total = optapy.constraint.DefaultConstraintMatchTotal(
                'NQueens',
                'Ascending Diagonal Conflict',
                optapy.score.SimpleScore.ONE)
            descending_diagonal_constraint_match_total = optapy.constraint.DefaultConstraintMatchTotal(
                'NQueens',
                'Descending Diagonal Conflict',
                optapy.score.SimpleScore.ONE)
            for row, queens in self.row_index_map.items():
                if len(queens) > 1:
                    row_conflict_constraint_match_total.addConstraintMatch(queens,
                                                                           optapy.score.SimpleScore.of(
                                                                               -len(queens) + 1))
            for row, queens in self.ascending_diagonal_index_map.items():
                if len(queens) > 1:
                    ascending_diagonal_constraint_match_total.addConstraintMatch(queens,
                                                                                 optapy.score.SimpleScore.of(
                                                                                     -len(queens) + 1))
            for row, queens in self.descending_diagonal_index_map.items():
                if len(queens) > 1:
                    descending_diagonal_constraint_match_total.addConstraintMatch(queens,
                                                                                  optapy.score.SimpleScore.of(
                                                                                      -len(queens) + 1))
            return [
                row_conflict_constraint_match_total,
                ascending_diagonal_constraint_match_total,
                descending_diagonal_constraint_match_total
            ]

        def getIndictmentMap(self):
            return None

    solver_config = optapy.config.solver.SolverConfig()
    termination_config = optapy.config.solver.termination.TerminationConfig()
    termination_config.setBestScoreLimit('0')
    solver_config.withSolutionClass(optapy.get_class(Solution)) \
        .withEntityClasses(optapy.get_class(Queen)) \
        .withScoreDirectorFactory(optapy.config.score.director.ScoreDirectorFactoryConfig() \
                                  .withIncrementalScoreCalculatorClass(optapy.get_class(IncrementalScoreCalculator))) \
        .withTerminationConfig(termination_config)
    problem: Solution = Solution(4,
                                 [Queen('A', 0), Queen('B', 1), Queen('C', 2), Queen('D', 3)],
                                 [0, 1, 2, 3],
                                 [0, 1, 2, 3])
    solver_factory = optapy.solver_factory_create(solver_config)
    solver = solver_factory.buildSolver()
    solution = solver.solve(problem)
    assert solution.get_score().getScore() == 0
    for i in range(4):
        for j in range(i + 1, 4):
            left_queen = solution.queen_list[i]
            right_queen = solution.queen_list[j]
            assert left_queen.row is not None and right_queen.row is not None
            assert left_queen.row != right_queen.row
            assert left_queen.getAscendingDiagonalIndex() != right_queen.getAscendingDiagonalIndex()
            assert left_queen.getDescendingDiagonalIndex() != right_queen.getDescendingDiagonalIndex()

    score_manager = optapy.score_manager_create(solver_factory)
    constraint_match_total_map = score_manager.explainScore(solution).getConstraintMatchTotalMap()
    row_conflict = constraint_match_total_map.get('NQueens/Row Conflict')
    ascending_diagonal_conflict = constraint_match_total_map.get('NQueens/Ascending Diagonal Conflict')
    descending_diagonal_conflict = constraint_match_total_map.get('NQueens/Descending Diagonal Conflict')
    assert row_conflict.getScore().getScore() == 0
    assert ascending_diagonal_conflict.getScore().getScore() == 0
    assert descending_diagonal_conflict.getScore().getScore() == 0

    bad_solution = Solution(4,
                            [Queen('A', 0, 0), Queen('B', 1, 1), Queen('C', 2, 0), Queen('D', 3, 1)],
                            [0, 1, 2, 3],
                            [0, 1, 2, 3])
    score_explanation = score_manager.explainScore(bad_solution)
    assert score_explanation.getScore().getScore() == -5
    constraint_match_total_map = score_explanation.getConstraintMatchTotalMap()
    row_conflict = constraint_match_total_map.get('NQueens/Row Conflict')
    ascending_diagonal_conflict = constraint_match_total_map.get('NQueens/Ascending Diagonal Conflict')
    descending_diagonal_conflict = constraint_match_total_map.get('NQueens/Descending Diagonal Conflict')
    assert row_conflict.getScore().getScore() == -2  # (A, C), (B, D)
    assert ascending_diagonal_conflict.getScore().getScore() == -1  # (B, C)
    assert descending_diagonal_conflict.getScore().getScore() == -2  # (A, B), (C, D)
    indictment_map = score_explanation.getIndictmentMap()
    assert indictment_map.get(bad_solution.queen_list[0]).getConstraintMatchCount() == 2
    assert indictment_map.get(bad_solution.queen_list[1]).getConstraintMatchCount() == 3
    assert indictment_map.get(bad_solution.queen_list[2]).getConstraintMatchCount() == 3
    assert indictment_map.get(bad_solution.queen_list[3]).getConstraintMatchCount() == 2
