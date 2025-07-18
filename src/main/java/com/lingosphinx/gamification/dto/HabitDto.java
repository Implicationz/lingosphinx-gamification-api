package com.lingosphinx.gamification.dto;

import lombok.*;

@Builder
@RequiredArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class HabitDto {
    private Long id;
    private GoalDto goal;
    @Builder.Default
    private StreakDto streak = new StreakDto();
}
