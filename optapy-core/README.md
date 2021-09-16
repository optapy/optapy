# OptaPy

[![Binder](https://mybinder.org/badge_logo.svg "Launch on Binder")](https://mybinder.org/v2/gh/optapy/optapy/main?filepath=index.ipynb)

OptaPy is *an AI constraint solver for Python* to optimize
the Vehicle Routing Problem, Employee Rostering, Maintenance Scheduling, Task Assignment, School Timetabling,
Cloud Optimization, Conference Scheduling, Job Shop Scheduling, Bin Packing and many more planning problems.

OptaPy wraps the [OptaPlanner](https://www.optaplanner.org/) engine internally,
but using OptaPy in Python is significantly slower than using OptaPlanner in Java or Kotlin.

>**WARNING**: OptaPy is an experimental technology.
>It is at least 20 times slower than using OptaPlanner in Java or Kotlin.

## Requirements

- [Install Python 3.9 or later.](https://www.python.org)
- [Install JDK 11 or later](https://adoptopenjdk.net) with the environment variable `JAVA_HOME` configured to the JDK installation directory.

## Source code overview

### Domain

In OptaPy, the domain has three parts:

- Problem Facts, which do not change
- Planning Entities, which have one or more planning variables
- Planning Solution, which define the facts and entities of the problem

#### Problem Facts

To declare Problem Facts, use the `@problem_fact` decorator

```python
from optapy import problem_fact
@problem_fact
class Timeslot:
    def __init__(self, id, dayOfWeek, startTime, endTime):
        self.id = id
        self.dayOfWeek = dayOfWeek
        self.startTime = startTime
        self.endTime = endTime
```

#### Planning Entities

To declare Planning Entities, use the `@planning_entity` decorator

```python
from optapy import planning_entity, planning_id, planning_variable

@planning_entity
class Lesson:
    def __init__(self, id, subject, teacher, studentGroup, timeslot=None, room=None):
        self.id = id
        self.subject = subject
        self.teacher = teacher
        self.studentGroup = studentGroup
        self.timeslot = timeslot
        self.room = room

    @planning_id
    def getId(self):
        return self.id

    @planning_variable(Timeslot, value_range_provider_refs=["timeslotRange"])
    def getTimeslot(self):
        return self.timeslot

    def setTimeslot(self, newTimeslot):
        self.timeslot = newTimeslot

    @planning_variable(Room, value_range_provider_refs=["roomRange"])
    def getRoom(self):
        return self.room

    def setRoom(self, newRoom):
        self.room = newRoom
```

- `@planning_variable` method decorators are used to indicate what fields can change. The method MUST follow JavaBean style conventions and have a corresponding setter (i.e. `getRoom(self)`, `setRoom(self, newRoom)`). The first parameter of the decorator is the type of the Planning Variable (required). The `value_range_provider_refs` parameter tells OptaPlanner what value range providers on the Planning Solution this Planning Variable can take values from.

- `@planning_id` is used to uniquely identify an entity object of a particular class. The same Planning Id can be used on entities of different classes, but the ids of all entities in the same class must be different.

#### Planning Solution

To declare the Planning Solution, use the `@planning_solution` decorator

```python
from optapy import planning_solution, problem_fact_collection_property, value_range_provider, planning_entity_collection_property, planning_score

@planning_solution
class TimeTable:
    def __init__(self, timeslotList=[], roomList=[], lessonList=[], score=None):
        self.timeslotList = timeslotList
        self.roomList = roomList
        self.lessonList = lessonList
        self.score = score

    @problem_fact_collection_property(Timeslot)
    @value_range_provider(range_id = "timeslotRange")
    def getTimeslotList(self):
        return self.timeslotList

    @problem_fact_collection_property(Room)
    @value_range_provider(range_id = "roomRange")
    def getRoomList(self):
        return self.roomList

    @planning_entity_collection_property(Lesson)
    def getLessonList(self):
        return self.lessonList

    @planning_score(HardSoftScore)
    def getScore(self):
        return self.score

    def setScore(self, score):
        self.score = score
```

- `@value_range_provider(range_id)` is used to indicate a method returns values a Planning Variable can take. It can be referenced by its id in the `value_range_provider_refs` parameter of `@planning_variable`. It should also have a `@problem_fact_collection_property` or a `@planning_entity_collection_property`.

- `@problem_fact_collection_property(type)` is used to indicate a method returns Problem Facts. The first parameter of the decorator is the type of the Problem Fact Collection (required). It should be a list.

- `@planning_entity_collection_property(type)` is used to indicate a method returns Planning Entities. The first parameter of the decorator is the type of the Planning Entity Collection (required). It should be a list.

- `@planning_score(scoreType)` is used to tell OptaPlanner what field holds the score. The method MUST follow JavaBean style conventions and have a corresponding setter (i.e. `getScore(self)`, `setScore(self, score)`). The first parameter of the decorator is the score type (required).

### Constraints

You define your constraints by using the ConstraintFactory
```python
from domain import Lesson
from optapy import get_class, constraint_provider
from optapy.types import Joiners, HardSoftScore

# Get the Java class corresponding to the Lesson Python class
LessonClass = get_class(Lesson)

@constraint_provider
def defineConstraints(constraintFactory):
    return [
        # Hard constraints
        roomConflict(constraintFactory),
        # Other constraints here...
    ]

def roomConflict(constraintFactory):
    # A room can accommodate at most one lesson at the same time.
    return constraintFactory \
            .fromUniquePair(LessonClass, [
            # ... in the same timeslot ...
                Joiners.equal(lambda lesson: lesson.timeslot),
            # ... in the same room ...
                Joiners.equal(lambda lesson: lesson.room)]) \
            .penalize("Room conflict", HardSoftScore.ONE_HARD)
```
for more details on Constraint Streams, see https://docs.optaplanner.org/latest/optaplanner-docs/html_single/index.html#constraintStreams

NOTE: Since `from` is a keyword in Python, to use the `constraintFactory.from(class, [joiners...])` function, you access it like `constraintFactory.from_(class, [joiners...])`

### Solve

```python
from optapy import get_class, solve
from optapy.types import SolverConfig, Duration
from constraints import defineConstraints
from domain import TimeTable, Lesson, generateProblem

solverConfig = SolverConfig().withEntityClasses(get_class(Lesson)) \
    .withSolutionClass(get_class(TimeTable)) \
    .withConstraintProviderClass(get_class(defineConstraints)) \
    .withTerminationSpentLimit(Duration.ofSeconds(30))

solution = solve(solverConfig, generateProblem())
```

`solution` will be a `TimeTable` instance with planning
variables set to the final best solution found.

## More information

For a complete example, see [the school timetabling quickstart](https://github.com/optapy/optapy/tree/main/optapy-quickstarts/school-timetabling).
For a full API spec, visit [OptaPlanner Documentation](https://www.optaplanner.org/learn/documentation.html).
