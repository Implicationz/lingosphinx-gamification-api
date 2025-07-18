package com.lingosphinx.gamification;

import com.lingosphinx.gamification.domain.ProgressValue;
import com.lingosphinx.gamification.dto.GoalDefinitionDto;
import com.lingosphinx.gamification.dto.GoalDto;
import com.lingosphinx.gamification.dto.GoalTypeDto;
import com.lingosphinx.gamification.dto.GoalZoneDto;
import com.lingosphinx.gamification.repository.GoalRepository;
import com.lingosphinx.gamification.repository.GoalDefinitionRepository;
import com.lingosphinx.gamification.service.GoalService;
import com.lingosphinx.gamification.service.GoalDefinitionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class GoalServiceTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("test-db")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void dbProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private GoalService goalService;

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private GoalDefinitionService goalDefinitionService;

    @Autowired
    private GoalDefinitionRepository goalDefinitionRepository;

    @BeforeEach
    void cleanUp() {
        goalRepository.deleteAll();
        goalDefinitionRepository.deleteAll();
    }

    private GoalDefinitionDto createSampleGoalDefinition(String name) {
        GoalDefinitionDto dto = new GoalDefinitionDto();
        dto.setName(name);
        dto.setType(GoalTypeDto.builder().name("type_" + name).build());
        dto.setZone(GoalZoneDto.builder().name("zone_" + name).build());
        dto.setReference("ref_" + name);
        dto.setWorth(10);
        dto.setTarget(ProgressValue.valueOf(5));

        dto.setImage("img.png");
        return goalDefinitionService.create(dto);
    }

    @Test
    void createGoalDefinition_shouldPersistGoalDefinition() {
        var saved = createSampleGoalDefinition("TestGoal");
        assertNotNull(saved.getId());
        assertEquals("TestGoal", saved.getName());
        assertEquals("type_TestGoal", saved.getType().getName());
        assertEquals("zone_TestGoal", saved.getZone().getName());
        assertEquals("ref_TestGoal", saved.getReference());
    }

    private GoalDto createSampleGoal(String goalDefName, UUID userId) {
        GoalDefinitionDto def = createSampleGoalDefinition(goalDefName);
        GoalDto dto = new GoalDto();
        dto.setDefinition(def);
        dto.setUserId(userId);
        dto.setProgress(ProgressValue.valueOf(5));
        return goalService.create(dto);
    }

    @Test
    void createGoal_shouldPersistGoal() {
        var userId = UUID.randomUUID();
        var saved = createSampleGoal("TestGoal", userId);
        assertNotNull(saved.getId());
        assertEquals("TestGoal", saved.getDefinition().getName());
        assertEquals(userId, saved.getUserId());
    }

    @Test
    void readById_shouldReturnPersistedGoal() {
        var userId = UUID.randomUUID();
        var saved = createSampleGoal("ReadGoal", userId);
        var found = goalService.readById(saved.getId());
        assertNotNull(found);
        assertEquals("ReadGoal", found.getDefinition().getName());
    }

    @Test
    void readAll_shouldReturnAllGoals() {
        var userId1 = UUID.randomUUID();
        var userId2 = UUID.randomUUID();
        createSampleGoal("Goal1", userId1);
        createSampleGoal("Goal2", userId2);
        List<GoalDto> all = goalService.readAll();
        assertEquals(2, all.size());
        assertTrue(all.stream().anyMatch(g -> "Goal1".equals(g.getDefinition().getName())));
        assertTrue(all.stream().anyMatch(g -> "Goal2".equals(g.getDefinition().getName())));
    }

    @Test
    void delete_shouldRemoveGoal() {
        var userId3 = UUID.randomUUID();
        var saved = createSampleGoal("ToDelete", userId3);
        goalService.delete(saved.getId());
        assertThrows(Exception.class, () -> goalService.readById(saved.getId()));
    }
}