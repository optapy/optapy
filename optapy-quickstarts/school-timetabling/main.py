from functools import reduce

from optapy import get_class, solver_factory_create
from optapy.types import SolverConfig, Duration
from domain import TimeTable, Lesson, generate_problem
from constraints import define_constraints


def print_timetable(timetable: TimeTable):
    room_list = timetable.room_list
    lesson_list = timetable.lesson_list
    timeslot_room_lesson_triple_list = list(map(lambda the_lesson: (the_lesson.timeslot, the_lesson.room, the_lesson),
                                                filter(lambda the_lesson:
                                                       the_lesson.timeslot is not None and
                                                       the_lesson.room is not None,
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
                 map(lambda the_room: "{:<10}".format(the_room.name)[0:10], room_list),
                 "|            | "))
    print("|" + ("------------|" * (len(room_list) + 1)))
    for timeslot in timetable.timeslot_list:
        cell_list = list(map(lambda the_room: lesson_map.get(timeslot, {}).get(the_room, []),
                             room_list))
        out = "| " + (timeslot.day_of_week[0:3] + " " + str(timeslot.start_time))[0:10] + " | "
        for cell in cell_list:
            if len(cell) == 0:
                out += "           | "
            else:
                out += "{:<10}".format(reduce(lambda a, b: a + "," + b,
                                              map(lambda assigned_lesson: assigned_lesson.subject,
                                                  cell)))[0:10] + " | "
        print(out)
        out = "|            | "
        for cell in cell_list:
            if len(cell) == 0:
                out += "           | "
            else:
                out += "{:<10}".format(reduce(lambda a, b: a + "," + b,
                                              map(lambda assigned_lesson: assigned_lesson.teacher,
                                                  cell)))[0:10] + " | "
        print(out)
        out = "|            | "
        for cell in cell_list:
            if len(cell) == 0:
                out += "           | "
            else:
                out += "{:<10}".format(reduce(lambda a, b: a + "," + b,
                                              map(lambda assigned_lesson: assigned_lesson.student_group,
                                                  cell)))[0:10] + " | "
        print(out)
        print("|" + ("------------|" * (len(room_list) + 1)))
    unassigned_lessons = list(
        filter(lambda unassigned_lesson: unassigned_lesson.timeslot is None or unassigned_lesson.room is None,
               lesson_list))
    if len(unassigned_lessons) > 0:
        print()
        print("Unassigned lessons")
        for lesson in unassigned_lessons:
            print(" " + lesson.subject + " - " + lesson.teacher + " - " + lesson.student_group)


solver_config = SolverConfig().withEntityClasses(get_class(Lesson)) \
    .withSolutionClass(get_class(TimeTable)) \
    .withConstraintProviderClass(get_class(define_constraints)) \
    .withTerminationSpentLimit(Duration.ofSeconds(30))

solver = solver_factory_create(solver_config).buildSolver()

solution = solver.solve(generate_problem())

print_timetable(solution)
