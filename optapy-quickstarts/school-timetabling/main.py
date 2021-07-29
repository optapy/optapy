from functools import reduce

from optapy import get_class, solve
from optapy.types import SolverConfig, Duration
from domain import TimeTable, Lesson, generateProblem

try:
    # Check to determine if we are in GraalPython
    # (GraalPython and JPype use different name mangling methods
    # so the constraint files is very sightly different
    # (GraalPython uses ["from"](...), JPype uses from_(...))
    import java
    java.type('java.lang.String')
    from graalconstraints import defineConstraints
except:
    from constraints import defineConstraints


def print_timetable(timetable: TimeTable):
    room_list = timetable.roomList
    lesson_list = timetable.lessonList
    timeslot_room_lesson_triple_list = list(map(lambda lesson: (lesson.timeslot, lesson.room, lesson),
                          filter(lambda lesson: lesson.timeslot is not None and lesson.room is not None,
                                 lesson_list)))
    lesson_map = dict()
    for timeslot, room, lesson in timeslot_room_lesson_triple_list:
        if timeslot in lesson_map:
            if room in lesson_map[timeslot]:
                lesson_map[timeslot][room].append(lesson)
            else:
                lesson_map[timeslot][room] = [lesson]
        else:
            lesson_map[timeslot] = {room: [lesson]}

    print("|" + ("------------|" * (len(room_list) + 1)))
    print(reduce(lambda a, b: a + b + " | ",
                 map(lambda room: "{:<10}".format(room.name)[0:10], room_list),
                 "|            | "))
    print("|" + ("------------|" * (len(room_list) + 1)))
    for timeslot in timetable.timeslotList:
        cell_list = list(map(lambda room: lesson_map.get(timeslot, {}).get(room, []),
                             room_list))
        out = "| " + (timeslot.dayOfWeek[0:3] + " " + str(timeslot.startTime))[0:10] + " | "
        for cell in cell_list:
            if len(cell) == 0:
                out += "           | "
            else:
                out += "{:<10}".format(reduce(lambda a, b: a + "," + b,
                                              map(lambda lesson: lesson.subject,
                                                  cell)))[0:10] + " | "
        print(out)
        out = "|            | "
        for cell in cell_list:
            if len(cell) == 0:
                out += "           | "
            else:
                out += "{:<10}".format(reduce(lambda a, b: a + "," + b,
                                              map(lambda lesson: lesson.teacher,
                                                  cell)))[0:10] + " | "
        print(out)
        out = "|            | "
        for cell in cell_list:
            if len(cell) == 0:
                out += "           | "
            else:
                out += "{:<10}".format(reduce(lambda a, b: a + "," + b,
                                                     map(lambda lesson: lesson.studentGroup,
                                                         cell)))[0:10] + " | "
        print(out)
        print("|" + ("------------|" * (len(room_list) + 1)))
    unassigned_lessons = list(filter(lambda lesson: lesson.timeslot is None or lesson.room is None,
                                lesson_list))
    if len(unassigned_lessons) > 0:
        print()
        print("Unassigned lessons")
        for lesson in unassigned_lessons:
            print(" " + lesson.subject + " - " + lesson.teacher + " - " + lesson.studentGroup)


solverConfig = SolverConfig().withEntityClasses(get_class(Lesson)) \
    .withSolutionClass(get_class(TimeTable)) \
    .withConstraintProviderClass(get_class(defineConstraints)) \
    .withTerminationSpentLimit(Duration.ofSeconds(30))

solution = solve(solverConfig, generateProblem())

print_timetable(solution)