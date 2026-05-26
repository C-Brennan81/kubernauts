package com.kubernauts.service;

import com.kubernauts.model.PodStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ScenarioEngineTest {

    private GameStateService gameState;
    private ScenarioEngine engine;

    @BeforeEach
    void setUp() {
        gameState = mock(GameStateService.class);
        engine = new ScenarioEngine(gameState);
    }

    @Test
    void getScenario_validIndex_returnsScenario() {
        assertThat(engine.getScenario(0)).isNotNull();
        assertThat(engine.getScenario(0).title()).isEqualTo("Malfunction Loop");
    }

    @Test
    void getScenario_outOfBounds_returnsNull() {
        assertThat(engine.getScenario(999)).isNull();
    }

    @Test
    void totalScenarios_returns7() {
        assertThat(engine.totalScenarios()).isEqualTo(7);
    }

    @ParameterizedTest(name = "scenario {0} is level {1}")
    @CsvSource({"0,1", "1,1", "2,2", "3,2", "4,3", "5,3", "6,3"})
    void getLevelForScenario(int idx, int expectedLevel) {
        assertThat(engine.getLevelForScenario(idx)).isEqualTo(expectedLevel);
    }

    @Test
    void isLevelStart_scenario2_true() {
        assertThat(engine.isLevelStart(2)).isTrue();
    }

    @Test
    void isLevelStart_scenario4_true() {
        assertThat(engine.isLevelStart(4)).isTrue();
    }

    @Test
    void isLevelStart_scenario0_false() {
        assertThat(engine.isLevelStart(0)).isFalse();
    }

    @Test
    void isLevelStart_scenario1_false() {
        assertThat(engine.isLevelStart(1)).isFalse();
    }

    @ParameterizedTest(name = "scenario {0}: cmd={1} target={2} → win={3}")
    @CsvSource({
        "0, fix,     crew-alpha-1,   true",
        "0, fix,     wrong-pod,      false",
        "1, inspect, nav-system-1,   true",
        "1, scan,    nav-system-1,   false",
        "2, deploy,  life-support,   true",
        "3, isolate, module-beta,    true",
        "4, revert,  crew-quarters,  true",
        "6, status,  '',             true",
    })
    void checkWinCondition(int idx, String cmd, String target, boolean expected) {
        Map<String, String> params = target.isBlank() ? Map.of() : Map.of("target", target);
        assertThat(engine.checkWinCondition(1L, idx, cmd, params)).isEqualTo(expected);
    }

    @Test
    void setupScenario_0_setsCrashLoop() {
        engine.setupScenario(1L, 0);
        verify(gameState).setPodStatus(1L, "crew-alpha-1", PodStatus.CRASH_LOOP);
    }

    @Test
    void setupScenario_1_setsPending() {
        engine.setupScenario(1L, 1);
        verify(gameState).setPodStatus(1L, "nav-system-1", PodStatus.PENDING);
    }

    @Test
    void setupScenario_2_setsOomKilled() {
        engine.setupScenario(1L, 2);
        verify(gameState).setPodStatus(1L, "life-support-1", PodStatus.OOM_KILLED);
    }
}
