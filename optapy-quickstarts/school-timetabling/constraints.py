from optapy import ConstraintProvider, getClass, Joiners, HardSoftScore
from domain import TimeTable, Lesson, Room
from datetime import datetime, date, timedelta

TimeTableClass = getClass(TimeTable)
LessonClass = getClass(Lesson)
RoomClass = getClass(Room)

today = date.today()
def within30Mins(lesson1, lesson2):
    between = datetime.combine(today, lesson1.timeslot.endTime) - datetime.combine(today, lesson2.timeslot.startTime)
    return timedelta(minutes=0) <= between <= timedelta(minutes=30)

@ConstraintProvider
def defineConstraints(constraintFactory):
    return [
        # Hard constraints
        roomConflict(constraintFactory),
        teacherConflict(constraintFactory),
        studentGroupConflict(constraintFactory),
        # Soft constraints
        teacherRoomStability(constraintFactory),
        teacherTimeEfficiency(constraintFactory),
        studentGroupSubjectVariety(constraintFactory)
    ]

def roomConflict(constraintFactory):
    # A room can accommodate at most one lesson at the same time.
    return constraintFactory \
            .fromUniquePair(LessonClass,
            # ... in the same timeslot ...
                [Joiners.equal(lambda lesson: lesson.timeslot),
            # ... in the same room ...
                Joiners.equal(lambda lesson: lesson.room)]) \
            .penalize("Room conflict", HardSoftScore.ONE_HARD)


def teacherConflict(constraintFactory):
    # A teacher can teach at most one lesson at the same time.
    return constraintFactory \
                .fromUniquePair(LessonClass,
                        [Joiners.equal(lambda lesson: lesson.timeslot),
                        Joiners.equal(lambda lesson: lesson.teacher)]) \
                .penalize("Teacher conflict", HardSoftScore.ONE_HARD)

def studentGroupConflict(constraintFactory):
    # A student can attend at most one lesson at the same time.
    return constraintFactory \
            .fromUniquePair(LessonClass,
                [Joiners.equal(lambda lesson: lesson.timeslot),
                Joiners.equal(lambda lesson: lesson.studentGroup)]) \
            .penalize("Student group conflict", HardSoftScore.ONE_HARD)

def teacherRoomStability(constraintFactory):
    # A teacher prefers to teach in a single room.
    return constraintFactory \
                .fromUniquePair(LessonClass,
                        [Joiners.equal(lambda lesson: lesson.teacher)]) \
                .filter(lambda lesson1, lesson2: lesson1.room != lesson2.room) \
                .penalize("Teacher room stability", HardSoftScore.ONE_SOFT)

def teacherTimeEfficiency(constraintFactory):
    # A teacher prefers to teach sequential lessons and dislikes gaps between lessons.
    return constraintFactory.from_(LessonClass)\
                .join(LessonClass, [Joiners.equal(lambda lesson: lesson.teacher),
                        Joiners.equal(lambda lesson: lesson.timeslot.dayOfWeek)]) \
                .filter(within30Mins) \
                .reward("Teacher time efficiency", HardSoftScore.ONE_SOFT)

def studentGroupSubjectVariety(constraintFactory):
    # A student group dislikes sequential lessons on the same subject.
    return constraintFactory.from_(LessonClass) \
        .join(LessonClass,
                        [Joiners.equal(lambda lesson: lesson.subject),
                        Joiners.equal(lambda lesson: lesson.studentGroup),
                        Joiners.equal(lambda lesson: lesson.timeslot.dayOfWeek)]) \
        .filter(within30Mins) \
        .penalize("Student group subject variety", HardSoftScore.ONE_SOFT)