package ru.yandex.practicum.gym;

import java.util.*;
import java.util.stream.Collectors;

public class Timetable {

    private Map<DayOfWeek, TreeMap<TimeOfDay, List<TrainingSession>>> timetable;

    public Timetable() {
        timetable = new HashMap<>();
    }

    public void addNewTrainingSession(TrainingSession trainingSession) {
        DayOfWeek day = trainingSession.getDayOfWeek();
        TimeOfDay time = trainingSession.getTimeOfDay();
        Coach newCoach = trainingSession.getCoach();

        int newStart = toMinutes(time);
        int newEnd = newStart + trainingSession.getGroup().getDuration();

        // Получаем или создаём расписание на день
        TreeMap<TimeOfDay, List<TrainingSession>> daySchedule =
                timetable.computeIfAbsent(day, k -> new TreeMap<>());

        // Проверяем, нет ли у тренера пересекающейся по времени тренировки
        boolean coachBusy = daySchedule.values().stream()
                .flatMap(List::stream)
                .anyMatch(existing -> overlaps(existing, newCoach, newStart, newEnd));

        if (coachBusy) {
            throw new IllegalArgumentException(
                    "Тренер " + newCoach.getSurname() + " уже ведёт пересекающуюся тренировку в "
                            + day + " в " + time.getHours() + ":" + String.format("%02d", time.getMinutes())
            );
        }

        // Если тренер свободен — добавляем тренировку
        daySchedule.computeIfAbsent(time, k -> new ArrayList<>()).add(trainingSession);
    }

    private boolean overlaps(TrainingSession existing, Coach newCoach, int newStart, int newEnd) {
        if (!existing.getCoach().equals(newCoach)) {
            return false;
        }
        int existingStart = toMinutes(existing.getTimeOfDay());
        int existingEnd = existingStart + existing.getGroup().getDuration();
        return newStart < existingEnd && existingStart < newEnd;
    }

    private int toMinutes(TimeOfDay time) {
        return time.getHours() * 60 + time.getMinutes();
    }

    public TreeMap<TimeOfDay, List<TrainingSession>> getTrainingSessionsForDay(DayOfWeek dayOfWeek) {
        TreeMap<TimeOfDay, List<TrainingSession>> daySchedule = timetable.get(dayOfWeek);

        if (daySchedule == null || daySchedule.isEmpty()) {
            return new TreeMap<>(); // Возвращаем пустой TreeMap, если расписание пустое
        }

        return daySchedule;
    }

    public List<TrainingSession> getTrainingSessionsForDayAndTime(DayOfWeek dayOfWeek, TimeOfDay timeOfDay) {
        TreeMap<TimeOfDay, List<TrainingSession>> trainingsInDay = timetable.get(dayOfWeek);
        if (trainingsInDay == null) return new ArrayList<TrainingSession>();
        List<TrainingSession> trainingSessionsForDayAndTime = trainingsInDay.get(timeOfDay);
        if (trainingSessionsForDayAndTime == null) return new ArrayList<TrainingSession>();
        return trainingSessionsForDayAndTime;
    }

    public Map<Coach, Integer> getCountByCoaches() {
        Map<Coach, Integer> coachCounts = new HashMap<>();

        // Проходим по всем дням
        for (TreeMap<TimeOfDay, List<TrainingSession>> daySchedule : timetable.values()) {
            // Проходим по всем временным слотам
            for (List<TrainingSession> sessions : daySchedule.values()) {
                // Проходим по всем тренировкам в слоте
                for (TrainingSession session : sessions) {
                    Coach coach = session.getCoach();
                    coachCounts.merge(coach, 1, Integer::sum);
                }
            }
        }

        // Сортируем по количеству тренировок
        return coachCounts.entrySet()
                .stream()
                .sorted(Map.Entry.<Coach, Integer>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new));
    }
}