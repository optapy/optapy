from optapy import *
from optapy.types import HardSoftScore
from datetime import time
from functools import reduce

@problem_fact
class Room:
    def __init__(self, id, name):
        self.id = id
        self.name = name

    @planning_id
    def getId(self):
        return self.id

    def __str__(self):
        return "Room(id=" + str(self.id) + ", name=" + str(self.name) + ")"

@problem_fact
class Timeslot:
    def __init__(self, id, dayOfWeek, startTime, endTime):
        self.id = id
        self.dayOfWeek = dayOfWeek
        self.startTime = startTime
        self.endTime = endTime

    @planning_id
    def getId(self):
        return self.id

    def __str__(self):
        return "Timeslot(id=" + str(self.id) + \
               ", dayOfWeek=" + str(self.dayOfWeek) + ", startTime=" + str(self.startTime) + \
               ", endTime=" + str(self.endTime) + ")"

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

    def __str__(self):
        return "Lesson(id=" + str(self.id) + \
                ", timeslot=" + str(self.timeslot) + ", room=" + str(self.room) + \
                ", teacher=" + str(self.teacher) + ", subject=" + str(self.subject) + \
                ", studentGroup=" + str(self.studentGroup) + ")"

# For better toString; list default __str__ only print address for some reason
def listString(aList):
    def itemConcat(soFar, newItem):
        return soFar + ",\n" + str(newItem)
    if len(aList) == 0:
        return "[]"
    elif len(aList) == 1:
        return "[" + str(aList[0]) + "]"
    else:
        return "[" + reduce(itemConcat, aList[1:], str(aList[0])) + "]"

@planning_solution
class TimeTable:
    def __init__(self, timeslotList=[], roomList=[], lessonList=[], score=None):
        self.timeslotList = timeslotList
        self.roomList = roomList
        self.lessonList = lessonList
        self.score = score

    @problem_fact_collection_property(Timeslot)
    @value_range_provider("timeslotRange")
    def getTimeslotList(self):
        return self.timeslotList

    @problem_fact_collection_property(Room)
    @value_range_provider("roomRange")
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

    def __str__(self):
        return "TimeTable(timeSlotList=" + listString(self.timeslotList) + \
               ",\nroomList=" + listString(self.roomList) + ",\nlessonList=" + listString(self.lessonList) + \
               ",\nscore=" + str(self.score.toString()) + ")"

def generateProblem():
    timeslotList = [
        Timeslot(1, "MONDAY", time(hour=8, minute=30), time(hour=9, minute=30)),
        Timeslot(2, "MONDAY", time(hour=9, minute=30), time(hour=10, minute=30)),
        Timeslot(3, "MONDAY", time(hour=10, minute=30), time(hour=11, minute=30)),
        Timeslot(4, "MONDAY", time(hour=13, minute=30), time(hour=14, minute=30)),
        Timeslot(5, "MONDAY", time(hour=14, minute=30), time(hour=15, minute=30)),
        Timeslot(6, "TUESDAY", time(hour=8, minute=30), time(hour=9, minute=30)),
        Timeslot(7, "TUESDAY", time(hour=9, minute=30), time(hour=10, minute=30)),
        Timeslot(8, "TUESDAY", time(hour=10, minute=30), time(hour=11, minute=30)),
        Timeslot(9, "TUESDAY", time(hour=13, minute=30), time(hour=14, minute=30)),
        Timeslot(10, "TUESDAY", time(hour=14, minute=30), time(hour=15, minute=30)),
    ]
    roomList = [
        Room(1, "Room A"),
        Room(2, "Room B"),
        Room(3, "Room C")
    ]
    lessonList = [
        Lesson(1, "Math", "A. Turing", "9th grade"),
        Lesson(2, "Math", "A. Turing", "9th grade"),
        Lesson(3, "Physics", "M. Curie", "9th grade"),
        Lesson(4, "Chemistry", "M. Curie", "9th grade"),
        Lesson(5, "Biology", "C. Darwin", "9th grade"),
        Lesson(6, "History", "I. Jones", "9th grade"),
        Lesson(7, "English", "I. Jones", "9th grade"),
        Lesson(8, "English", "I. Jones", "9th grade"),
        Lesson(9, "Spanish", "P. Cruz", "9th grade"),
        Lesson(10, "Spanish", "P. Cruz", "9th grade"),
        Lesson(11, "Math", "A. Turing", "10th grade"),
        Lesson(12, "Math", "A. Turing", "10th grade"),
        Lesson(13, "Math", "A. Turing", "10th grade"),
        Lesson(14, "Physics", "M. Curie", "10th grade"),
        Lesson(15, "Chemistry", "M. Curie", "10th grade"),
        Lesson(16, "French", "M. Curie", "10th grade"),
        Lesson(17, "Geography", "C. Darwin", "10th grade"),
        Lesson(18, "History", "I. Jones", "10th grade"),
        Lesson(19, "English", "P. Cruz", "10th grade"),
        Lesson(20, "Spanish", "P. Cruz", "10th grade"),
    ]
    lesson = lessonList[0]
    lesson.setTimeslot(timeslotList[0])
    lesson.setRoom(roomList[0])

    return TimeTable(timeslotList, roomList, lessonList)