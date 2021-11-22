import optapy
import optapy.score
from abc import ABC, abstractmethod

# Using Testdata as prefix to name causes PyTest warnings because it think it is a test class


@optapy.problem_fact
class ExampleValue:
    def __init__(self, code):
        self.code = code


@optapy.planning_entity
class ExampleEntity:
    def __init__(self, code, value):
        self.code = code
        self.value = value

    @optapy.planning_variable(ExampleValue, value_range_provider_refs=['value_range'])
    def get_value(self):
        return self.value

    def set_value(self, value):
        self.value = value


class ExampleExtendedEntity(ExampleEntity):
    def __init__(self, extra_object, code, value):
        super().__init__(code, value)
        self.extra_object = extra_object


class ThirdpartyEntity:
    def __init__(self, code, value):
        self.code = code
        self.value = value

    def get_value(self):
        return self.value

    def set_value(self, value):
        self.value = value


@optapy.planning_entity
class ExampleExtendedThirdpartyEntity(ThirdpartyEntity):
    def __init__(self, extra_object, code, value):
        super().__init__(code, value)
        self.extra_object = extra_object

    @optapy.planning_variable(ExampleValue, value_range_provider_refs=['value_range'])
    def get_value(self):
        return super().get_value()

    def set_value(self, value):
        super().set_value(value)


@optapy.planning_solution
class ExampleSolution:
    def __init__(self, code, value_list, entity_list, score=None):
        self.code = code
        self.value_list = value_list
        self.entity_list = entity_list
        self.score = score

    @optapy.problem_fact_collection_property(ExampleValue)
    @optapy.value_range_provider('value_range')
    def get_value_list(self):
        return self.value_list

    @optapy.planning_entity_collection_property(ExampleEntity)
    def get_entity_list(self):
        return self.entity_list

    @optapy.planning_score(optapy.score.SimpleScore)
    def get_score(self):
        return self.score

    def set_score(self, score):
        self.score = score


@optapy.planning_solution
class ExampleExtendedSolution(ExampleSolution):
    def __init__(self, extra_object, code, value_list, entity_list, score=None):
        super().__init__(code, value_list, entity_list, score)
        self.extra_object = extra_object


class ThirdPartySolution:
    def __init__(self, code, value_list, entity_list):
        self.code = code
        self.value_list = value_list
        self.entity_list = entity_list

    def get_value_list(self):
        return self.value_list

    def get_entity_list(self):
        return self.entity_list


@optapy.planning_solution
class ExampleExtendedThirdPartySolution(ThirdPartySolution):
    def __init__(self, extra_object, code, value_list, entity_list, score=None):
        super().__init__(code, value_list, entity_list)
        self.extra_object = extra_object
        self.score = score

    @optapy.problem_fact_collection_property(ExampleValue)
    @optapy.value_range_provider('value_range')
    def get_value_list(self):
        return super().get_value_List()

    @optapy.planning_entity_collection_property(ExampleEntity)
    def get_entity_list(self):
        return super().get_entity_list()

    @optapy.planning_score(optapy.score.SimpleScore)
    def get_score(self):
        return self.score

    def set_score(self, score):
        self.score = score


def test_clone_solution():
    val1 = ExampleValue("1")
    val2 = ExampleValue("2")
    val3 = ExampleValue("3")
    a = ExampleEntity("a", val1)
    b = ExampleEntity("b", val1)
    c = ExampleEntity("c", val3)
    d = ExampleEntity("d", val3)

    original_value_list = [val1, val2, val3]
    original_entity_list = [a, b, c, d]
    original_score = optapy.score.SimpleScore.ONE

    original_solution = ExampleSolution("solution", original_value_list, original_entity_list, original_score)

    clone_solution = optapy._planning_clone(original_solution, dict())
    assert original_solution is not clone_solution
    assert original_solution.code == clone_solution.code
    assert clone_solution.value_list is original_value_list
    assert clone_solution.entity_list is not original_entity_list
    assert clone_solution.score is original_solution.score
    assert len(clone_solution.entity_list) == len(original_solution.entity_list)
    for i in range(len(clone_solution.entity_list)):
        clone_entity = clone_solution.entity_list[i]
        original_entity = original_entity_list[i]
        assert clone_entity is not original_entity
        assert clone_entity.code == original_entity.code
        assert clone_entity.value is original_entity.value
    clone_solution.get_entity_list()[1].set_value(val2)
    assert clone_solution.get_entity_list()[1].get_value() is val2
    assert b.get_value() is val1


def test_clone_extended_solution():
    val1 = ExampleValue("1")
    val2 = ExampleValue("2")
    val3 = ExampleValue("3")
    a = ExampleExtendedEntity(1, "a", val1)
    b = ExampleExtendedEntity(2, "b", val1)
    c = ExampleExtendedEntity(3, "c", val3)
    d = ExampleExtendedEntity(4, "d", val3)

    original_value_list = [val1, val2, val3]
    original_entity_list = [a, b, c, d]
    original_score = optapy.score.SimpleScore.ONE
    original_extra_object = "extra"

    original_solution = ExampleExtendedSolution(original_extra_object, "solution", original_value_list,
                                                original_entity_list, original_score)

    clone_solution = optapy._planning_clone(original_solution, dict())
    assert original_solution is not clone_solution
    assert original_solution.extra_object is clone_solution.extra_object
    assert original_solution.code == clone_solution.code
    assert clone_solution.value_list is original_value_list
    assert clone_solution.entity_list is not original_entity_list
    assert clone_solution.score is original_solution.score
    assert len(clone_solution.entity_list) == len(original_solution.entity_list)
    for i in range(len(clone_solution.entity_list)):
        clone_entity = clone_solution.entity_list[i]
        original_entity = original_entity_list[i]
        assert clone_entity is not original_entity
        assert clone_entity.extra_object is original_entity.extra_object
        assert clone_entity.code == original_entity.code
        assert clone_entity.value is original_entity.value
    clone_solution.get_entity_list()[1].set_value(val2)
    assert clone_solution.get_entity_list()[1].get_value() is val2
    assert b.get_value() is val1


def test_clone_extended_thirdparty_solution():
    val1 = ExampleValue("1")
    val2 = ExampleValue("2")
    val3 = ExampleValue("3")
    a = ExampleExtendedThirdpartyEntity(1, "a", val1)
    b = ExampleExtendedThirdpartyEntity(2, "b", val1)
    c = ExampleExtendedThirdpartyEntity(3, "c", val3)
    d = ExampleExtendedThirdpartyEntity(4, "d", val3)

    original_value_list = [val1, val2, val3]
    original_entity_list = [a, b, c, d]
    original_score = optapy.score.SimpleScore.ONE
    original_extra_object = "extra"

    original_solution = ExampleExtendedThirdPartySolution(original_extra_object, "solution", original_value_list,
                                                          original_entity_list, original_score)

    clone_solution = optapy._planning_clone(original_solution, dict())
    assert original_solution is not clone_solution
    assert original_solution.extra_object is clone_solution.extra_object
    assert original_solution.code == clone_solution.code
    assert clone_solution.value_list is original_value_list
    assert clone_solution.entity_list is not original_entity_list
    assert clone_solution.score is original_solution.score
    assert len(clone_solution.entity_list) == len(original_solution.entity_list)
    for i in range(len(clone_solution.entity_list)):
        clone_entity = clone_solution.entity_list[i]
        original_entity = original_entity_list[i]
        assert clone_entity is not original_entity
        assert clone_entity.extra_object is original_entity.extra_object
        assert clone_entity.code == original_entity.code
        assert clone_entity.value is original_entity.value
    clone_solution.get_entity_list()[1].set_value(val2)
    assert clone_solution.get_entity_list()[1].get_value() is val2
    assert b.get_value() is val1


@optapy.problem_fact
class ExampleChainedObject:
    pass


@optapy.problem_fact
class ExampleChainedAnchor:
    def __init__(self, code):
        self.code = code


@optapy.planning_entity
class ExampleChainedEntity:
    def __init__(self, code, chained_object, unchained_value=None):
        self.code = code
        self.chained_object = chained_object
        self.unchained_value = unchained_value

    @optapy.planning_variable(ExampleChainedObject, value_range_provider_refs=['chained_anchor_range',
                                                                               'chained_entity_range'])
    def get_chained_object(self):
        return self.chained_object

    def set_chained_object(self, chained_object):
        self.chained_object = chained_object

    @optapy.planning_variable(ExampleValue, value_range_provider_refs=['unchained_range'])
    def get_unchained_value(self):
        return self.unchained_value

    def set_unchained_value(self, unchained_value):
        self.unchained_value = unchained_value


@optapy.planning_solution
class ExampleChainedSolution:
    def __init__(self, code, chained_anchor_list, chained_entity_list, unchained_value_list, score=None):
        self.code = code
        self.chained_anchor_list = chained_anchor_list
        self.chained_entity_list = chained_entity_list
        self.unchained_value_list = unchained_value_list
        self.score = score

    @optapy.value_range_provider('chained_anchor_range')
    @optapy.problem_fact_collection_property(ExampleChainedAnchor)
    def get_chained_anchor_list(self):
        return self.chained_anchor_list

    @optapy.value_range_provider('chained_entity_range')
    @optapy.planning_entity_collection_property(ExampleChainedEntity)
    def get_chained_entity_list(self):
        return self.chained_entity_list

    @optapy.value_range_provider('unchained_range')
    def get_unchained_value_list(self):
        return self.unchained_value_list

    @optapy.planning_score(optapy.score.SimpleScore)
    def get_score(self):
        return self.score

    def set_score(self, score):
        self.score = score


def assert_chained_entity_clone(original_entity, clone_entity, entity_code, value):
    assert clone_entity is not original_entity
    assert entity_code == original_entity.code
    assert entity_code == clone_entity.code
    assert clone_entity.chained_object is value


def test_clone_chained_solution():
    a0 = ExampleChainedAnchor('a0')
    a1 = ExampleChainedEntity('a1', a0)
    a2 = ExampleChainedEntity('a2', a1)
    a3 = ExampleChainedEntity('a3', a2)

    b0 = ExampleChainedAnchor('b0')
    b1 = ExampleChainedEntity('b1', b0)

    original_score = optapy.score.SimpleScore.ONE
    anchor_list = [a0, b0]
    entity_list = [a1, a2, a3, b1]

    original_solution = ExampleChainedSolution('solution', anchor_list, entity_list, [], original_score)
    clone_solution = optapy._planning_clone(original_solution, dict())
    assert clone_solution is not original_solution
    assert clone_solution.code is original_solution.code
    assert clone_solution.chained_anchor_list is anchor_list
    assert clone_solution.score is original_score

    assert clone_solution.chained_entity_list is not original_solution.chained_entity_list
    assert len(clone_solution.chained_entity_list) == 4

    clone_a1 = clone_solution.chained_entity_list[0]
    clone_a2 = clone_solution.chained_entity_list[1]
    clone_a3 = clone_solution.chained_entity_list[2]
    clone_b1 = clone_solution.chained_entity_list[3]

    assert_chained_entity_clone(a1, clone_a1, "a1", a0)
    assert_chained_entity_clone(a2, clone_a2, "a2", clone_a1)
    assert_chained_entity_clone(a3, clone_a3, "a3", clone_a2)
    assert_chained_entity_clone(b1, clone_b1, "b1", b0)

    a3.set_chained_object(b1)
    assert a3.chained_object.code == 'b1'
    assert clone_a3.chained_object.code == 'a2'


@optapy.planning_entity
class ExampleShadowingChainedObject(ABC):
    @abstractmethod
    @optapy.inverse_relation_shadow_variable('chained_object', is_singleton=True)
    def get_next_entity(self):
        pass

    @abstractmethod
    def set_next_entity(self, next_entity):
        pass


@optapy.planning_entity
class ExampleShadowingChainedAnchor(ExampleShadowingChainedObject):
    def __init__(self, code, next_entity=None):
        self.code = code
        self.next_entity = next_entity

    def get_next_entity(self):
        return self.next_entity

    def set_next_entity(self, next_entity):
        self.next_entity = next_entity

    def __str__(self):
        return self.code


@optapy.planning_entity
class ExampleShadowingChainedEntity(ExampleShadowingChainedObject):
    def __init__(self, code, chained_object, unchained_value=None, anchor=None, next_entity=None):
        self.code = code
        self.chained_object = chained_object
        self.unchained_value = unchained_value
        self.anchor = anchor
        self.next_entity = next_entity

    @optapy.planning_variable(ExampleChainedObject, value_range_provider_refs=['chained_anchor_range',
                                                                               'chained_entity_range'])
    def get_chained_object(self):
        return self.chained_object

    def set_chained_object(self, chained_object):
        self.chained_object = chained_object

    @optapy.planning_variable(ExampleValue, value_range_provider_refs=['unchained_range'])
    def get_unchained_value(self):
        return self.unchained_value

    def set_unchained_value(self, unchained_value):
        self.unchained_value = unchained_value

    def get_next_entity(self):
        return self.next_entity

    def set_next_entity(self, next_entity):
        self.next_entity = next_entity

    @optapy.anchor_shadow_variable('chained_object')
    def get_anchor(self):
        return self.anchor

    def set_anchor(self, anchor):
        self.anchor = anchor

    def __str__(self):
        return self.code


@optapy.planning_solution
class ExampleShadowingChainedSolution:
    def __init__(self, code, chained_anchor_list, chained_entity_list, unchained_value_list, score=None):
        self.code = code
        self.chained_anchor_list = chained_anchor_list
        self.chained_entity_list = chained_entity_list
        self.unchained_value_list = unchained_value_list
        self.score = score

    @optapy.value_range_provider('chained_anchor_range')
    @optapy.planning_entity_collection_property(ExampleShadowingChainedAnchor)
    def get_chained_anchor_list(self):
        return self.chained_anchor_list

    @optapy.value_range_provider('chained_entity_range')
    @optapy.planning_entity_collection_property(ExampleShadowingChainedEntity)
    def get_chained_entity_list(self):
        return self.chained_entity_list

    @optapy.value_range_provider('unchained_range')
    def get_unchained_value_list(self):
        return self.unchained_value_list

    @optapy.planning_score(optapy.score.SimpleScore)
    def get_score(self):
        return self.score

    def set_score(self, score):
        self.score = score


def assert_shadowing_chained_anchor_clone(original_entity, clone_entity, entity_code, next_entity):
    assert clone_entity is not original_entity
    assert entity_code == original_entity.code
    assert entity_code == clone_entity.code
    assert clone_entity.next_entity is next_entity


def assert_shadowing_chained_entity_clone(original_entity, clone_entity, entity_code, value, next_entity):
    assert clone_entity is not original_entity
    assert entity_code == original_entity.code
    assert entity_code == clone_entity.code
    assert clone_entity.chained_object is value
    assert clone_entity.next_entity is next_entity


def test_clone_chained_shadowing_solution():
    a0 = ExampleShadowingChainedAnchor('a0')
    a1 = ExampleShadowingChainedEntity('a1', a0)
    a2 = ExampleShadowingChainedEntity('a2', a1)
    a3 = ExampleShadowingChainedEntity('a3', a2)

    b0 = ExampleShadowingChainedAnchor('b0')
    b1 = ExampleShadowingChainedEntity('b1', b0)

    a0.set_next_entity(a1)
    a1.set_next_entity(a2)
    a2.set_next_entity(a3)
    a3.set_next_entity(None)

    b0.set_next_entity(b1)
    b1.set_next_entity(None)

    original_score = optapy.score.SimpleScore.ONE
    anchor_list = [a0, b0]
    entity_list = [a1, a2, a3, b1]

    original_solution = ExampleShadowingChainedSolution('solution', anchor_list, entity_list, [], original_score)
    clone_solution = optapy._planning_clone(original_solution, dict())
    assert clone_solution is not original_solution
    assert clone_solution.code is original_solution.code
    assert clone_solution.score is original_score

    assert clone_solution.chained_anchor_list is not anchor_list
    assert clone_solution.chained_entity_list is not original_solution.chained_entity_list
    assert len(clone_solution.chained_anchor_list) == 2
    assert len(clone_solution.chained_entity_list) == 4

    clone_a0 = clone_solution.chained_anchor_list[0]
    clone_a1 = clone_solution.chained_entity_list[0]
    clone_a2 = clone_solution.chained_entity_list[1]
    clone_a3 = clone_solution.chained_entity_list[2]

    clone_b0 = clone_solution.chained_anchor_list[1]
    clone_b1 = clone_solution.chained_entity_list[3]

    assert_shadowing_chained_anchor_clone(a0, clone_a0, "a0", clone_a1)
    assert_shadowing_chained_entity_clone(a1, clone_a1, "a1", clone_a0, clone_a2)
    assert_shadowing_chained_entity_clone(a2, clone_a2, "a2", clone_a1, clone_a3)
    assert_shadowing_chained_entity_clone(a3, clone_a3, "a3", clone_a2, None)
    assert_shadowing_chained_anchor_clone(b0, clone_b0, "b0", clone_b1)
    assert_shadowing_chained_entity_clone(b1, clone_b1, "b1", clone_b0, None)

    a3.set_chained_object(b1)
    assert a3.chained_object.code == 'b1'
    assert clone_a3.chained_object.code == 'a2'


@optapy.planning_entity
class ExampleDeepCloningEntity:
    def __init__(self, code, value, shadow_variable_list, shadow_variable_map):
        self.code = code
        self.value = value
        self.shadow_variable_list = shadow_variable_list
        self.shadow_variable_map = shadow_variable_map

    @optapy.planning_variable(ExampleValue, value_range_provider_refs=['value_range'])
    def get_value(self):
        return self.value

    def set_value(self, value):
        self.value = value

    @optapy.deep_planning_clone
    def get_shadow_variable_list(self):
        return self.shadow_variable_list

    def set_shadow_variable_list(self, shadow_variable_list):
        self.shadow_variable_list = shadow_variable_list

    @optapy.deep_planning_clone
    def get_shadow_variable_map(self):
        return self.shadow_variable_map

    def set_shadow_variable_map(self, shadow_variable_map):
        self.shadow_variable_map = shadow_variable_map


@optapy.planning_solution
class ExampleDeepCloningSolution:
    def __init__(self, code, value_list, entity_list, general_shadow_variable_list, score=None):
        self.code = code
        self.value_list = value_list,
        self.entity_list = entity_list
        self.general_shadow_variable_list = general_shadow_variable_list
        self.score = score

    @optapy.value_range_provider('value_range')
    @optapy.problem_fact_collection_property
    def get_value_list(self):
        return self.value_list

    @optapy.planning_entity_collection_property(ExampleDeepCloningEntity)
    def get_entity_list(self):
        return self.entity_list

    @optapy.deep_planning_clone
    def get_general_shadow_variable_list(self):
        return self.general_shadow_variable_list

    def set_general_shadow_variable_list(self, general_shadow_variable_list):
        self.general_shadow_variable_list = general_shadow_variable_list

    @optapy.planning_score(optapy.score.SimpleScore)
    def get_score(self):
        return self.score

    def set_score(self, score):
        self.score = score


def assert_deep_cloning_entity_clone(original_entity, clone_entity, entity_code):
    assert original_entity is not clone_entity
    assert original_entity.code == entity_code
    assert clone_entity.code == entity_code
    assert clone_entity.value is original_entity.value

    original_shadow_variable_list = original_entity.shadow_variable_list
    clone_shadow_variable_list = clone_entity.shadow_variable_list

    if original_shadow_variable_list is None:
        assert clone_shadow_variable_list is None
    else:
        assert clone_shadow_variable_list is not original_shadow_variable_list
        assert len(clone_shadow_variable_list) == len(original_shadow_variable_list)
        for i in range(len(original_shadow_variable_list)):
            assert clone_shadow_variable_list[i] is original_shadow_variable_list[i]

    original_shadow_variable_map = original_entity.shadow_variable_map
    clone_shadow_variable_map = clone_entity.shadow_variable_map
    if original_shadow_variable_map is None:
        assert clone_shadow_variable_map is None
    else:
        assert clone_shadow_variable_map is not original_shadow_variable_map
        assert len(clone_shadow_variable_map) == len(original_shadow_variable_map)
        for key in original_shadow_variable_map.keys():
            assert clone_shadow_variable_map[key] is original_shadow_variable_map[key]


def test_deep_planning_clone():
    val1 = ExampleValue('1')
    val2 = ExampleValue('2')
    val3 = ExampleValue('3')
    a_shadow_variable_list = ['shadow a1', 'shadow a2']
    a = ExampleDeepCloningEntity('a', val1, a_shadow_variable_list, None)

    b_shadow_variable_map = {
        'shadow key b1': 'shadow value b1',
        'shadow key b2': 'shadow value b2'
    }
    b = ExampleDeepCloningEntity('b', val1, None, b_shadow_variable_map)

    c_shadow_variable_list = ['shadow c1', 'shadow c2']
    c = ExampleDeepCloningEntity('c', val3, c_shadow_variable_list, None)
    d = ExampleDeepCloningEntity('d', val3, None, None)

    value_list = [val1, val2, val3]
    original_entity_list = [a, b, c, d]
    general_shadow_variable_list = ['shadow g1', 'shadow g2']
    original = ExampleDeepCloningSolution('solution', value_list, original_entity_list,
                                          general_shadow_variable_list, None)
    # ...somehow passing it to the constructor changes it?
    value_list = original.value_list
    clone = optapy._planning_clone(original, dict())

    assert clone is not original
    assert clone.code == 'solution'
    assert clone.value_list is value_list
    assert clone.score is None

    clone_entity_list = clone.entity_list
    assert clone_entity_list is not original_entity_list
    assert len(clone_entity_list) == 4
    clone_a = clone_entity_list[0]
    assert_deep_cloning_entity_clone(a, clone_a, 'a')
    clone_b = clone_entity_list[1]
    assert_deep_cloning_entity_clone(b, clone_b, 'b')
    clone_c = clone_entity_list[2]
    assert_deep_cloning_entity_clone(c, clone_c, 'c')
    clone_d = clone_entity_list[3]
    assert_deep_cloning_entity_clone(d, clone_d, 'd')

    clone_general_shadow_variable_list = clone.general_shadow_variable_list
    assert clone_general_shadow_variable_list is not general_shadow_variable_list
    assert len(clone_general_shadow_variable_list) == 2
    assert clone_general_shadow_variable_list[0] is general_shadow_variable_list[0]
    assert clone_general_shadow_variable_list[1] is general_shadow_variable_list[1]

    b.set_value(val2)
    assert b.value.code == '2'
    assert clone_b.value.code == '1'

    b.shadow_variable_map['shadow key b1'] = 'other shadow value b1'
    assert b.shadow_variable_map['shadow key b1'] == 'other shadow value b1'
    assert clone_b.shadow_variable_map['shadow key b1'] == 'shadow value b1'


@optapy.planning_entity
class ExampleSolutionBacklinkingEntity:
    def __init__(self, code, value, solution):
        self.code = code
        self.value = value
        self.solution = solution

    @optapy.planning_variable(ExampleValue, value_range_provider_refs=['value_range'])
    def get_value(self):
        return self.value

    def set_value(self, value):
        self.value = value


@optapy.planning_solution
class ExampleBacklinkingSolution:
    def __init__(self, code, value_list, entity_list, score=None):
        self.code = code
        self.value_list = value_list
        self.entity_list = entity_list
        self.score = score

    @optapy.problem_fact_collection_property(ExampleValue)
    @optapy.value_range_provider('value_range')
    def get_value_list(self):
        return self.value_list

    @optapy.planning_entity_collection_property(ExampleSolutionBacklinkingEntity)
    def get_entity_list(self):
        return self.entity_list

    @optapy.planning_score(optapy.score.SimpleScore)
    def get_score(self):
        return self.score

    def set_score(self, score):
        self.score = score


def test_supports_entity_to_solution_backlinking():
    val1 = ExampleValue("1")
    val2 = ExampleValue("2")
    original_value_list = [val1, val2]
    original_entity_list = []
    original_score = optapy.score.SimpleScore.ONE
    original_solution = ExampleSolution("solution", original_value_list, original_entity_list, original_score)
    a = ExampleSolutionBacklinkingEntity('A', None, original_solution)
    original_entity_list.append(a)
    clone_solution = optapy._planning_clone(original_solution, dict())
    assert a.solution is original_solution
    assert original_solution is not clone_solution
    assert original_solution.code == clone_solution.code
    assert clone_solution.value_list is original_value_list
    assert clone_solution.entity_list is not original_entity_list
    assert clone_solution.score is original_solution.score
    assert len(clone_solution.entity_list) == len(original_solution.entity_list)
    for i in range(len(clone_solution.entity_list)):
        clone_entity = clone_solution.entity_list[i]
        original_entity = original_entity_list[i]
        assert clone_entity is not original_entity
        assert clone_entity.code == original_entity.code
        assert clone_entity.value is original_entity.value
        assert clone_entity.solution is clone_solution
    clone_solution.get_entity_list()[0].set_value(val2)
    assert clone_solution.get_entity_list()[0].get_value() is val2
    assert a.get_value() is None
