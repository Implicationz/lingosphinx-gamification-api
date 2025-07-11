package com.lingosphinx.gamification.service;

import com.lingosphinx.gamification.domain.Habit;
import com.lingosphinx.gamification.domain.ProgressValue;
import com.lingosphinx.gamification.domain.RenewalType;
import com.lingosphinx.gamification.domain.StreakProgress;
import com.lingosphinx.gamification.dto.HabitDto;
import com.lingosphinx.gamification.dto.HabitDto;
import com.lingosphinx.gamification.mapper.HabitMapper;
import com.lingosphinx.gamification.repository.*;
import com.lingosphinx.gamification.repository.HabitSpecifications;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class HabitServiceImpl implements HabitService {

    private final HabitRepository habitRepository;
    private final HabitMapper habitMapper;

    private final StreakProgressRepository progressRepository;

    @Override
    public HabitDto create(HabitDto habitDto) {
        var habit = habitMapper.toEntity(habitDto);
        var savedHabit = habitRepository.save(habit);
        log.info("Habit created successfully: id={}", savedHabit.getId());
        return habitMapper.toDto(savedHabit);
    }

    @Transactional(readOnly = true)
    protected void fetchStreakHistory(Habit habit) {
        var streak = habit.getStreak();
        var tenDaysAgoStart = LocalDate.now()
                .minusDays(9)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();

        var spec = StreakProgressSpecifications.byStreakId(streak.getId())
                .and(StreakProgressSpecifications.timestampAfter(tenDaysAgoStart));

        var history = progressRepository.findAll(spec, Sort.by("timestamp"));
        streak.setHistory(history);
    }

    @Override
    @Transactional(readOnly = true)
    public HabitDto readById(Long id) {
        var habit = habitRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Habit not found"));
        fetchStreakHistory(habit);
        return this.habitMapper.toDto(habit);
    }

    @Override
    @Transactional(readOnly = true)
    public HabitDto readByTypeNameAndReference(String type, String reference) {
        var spec = HabitSpecifications.byTypeNameAndReference(type, reference);
        var habit = habitRepository.findOne(spec)
                .orElseThrow(() -> new EntityNotFoundException("Habit not found for type=" + type + " and reference=" + reference));
        log.info("Habit read by type and reference: type={}, reference={}, id={}", type, reference, habit.getId());
        fetchStreakHistory(habit);
        return habitMapper.toDto(habit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HabitDto> readAll() {
        var result = habitRepository.findAll().stream()
                .map(habitMapper::toDto)
                .toList();
        log.info("All habits read successfully, count={}", result.size());
        return result;
    }

    @Override
    public HabitDto update(Long id, HabitDto habitDto) {
        var existingHabit = habitRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Habit not found"));

        habitMapper.toEntityFromDto(habitDto, existingHabit);
        log.info("Habit updated successfully: id={}", existingHabit.getId());
        return habitMapper.toDto(existingHabit);
    }

    @Override
    public void delete(Long id) {
        habitRepository.deleteById(id);
        log.info("Habit deleted successfully: id={}", id);
    }

    public void reset(Habit habit) {
        habit.getStreak().setDuration(0L);
        habit.getGoal().setProgress(ProgressValue.ZERO);
        habitRepository.save(habit);
        log.info("Habit streak reset: id={}", habit.getId());

    }

    @Override
    public void resetAll() {
        var spec = HabitSpecifications.due();
        var habits = habitRepository.findAll(spec);
        habits.forEach(this::reset);
    }
}