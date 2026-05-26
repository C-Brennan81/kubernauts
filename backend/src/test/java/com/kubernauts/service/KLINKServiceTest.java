package com.kubernauts.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KLINKServiceTest {

    private KLINKService klink;

    @BeforeEach
    void setUp() { klink = new KLINKService(); }

    @Test
    void welcome_containsPlayerName() {
        assertThat(klink.welcome("Alice")).contains("Alice");
    }

    @Test
    void onUnknownCommand_containsInput() {
        assertThat(klink.onUnknownCommand("foobar")).contains("foobar");
    }

    @Test
    void onHelp_containsAllCommands() {
        String help = klink.onHelp();
        assertThat(help).contains("scan", "inspect", "fix", "deploy", "revert", "isolate", "status", "hint");
    }

    @Test
    void onHint_containsHintText() {
        assertThat(klink.onHint("Try: fix crew-alpha-1")).contains("Try: fix crew-alpha-1");
    }

    @Test
    void onScan_emptyDeployment_returnsNotFoundMessage() {
        assertThat(klink.onScan("ghost-deploy", List.of())).isNotBlank();
    }

    @Test
    void onScan_withUnits_containsUnitName() {
        Map<String, Object> unit = Map.of("name", "crew-alpha-1", "status", "CRASH_LOOP", "restarts", 3);
        assertThat(klink.onScan("crew-quarters", List.of(unit))).contains("crew-alpha-1");
    }

    @Test
    void onFix_success_returnsNonBlank() {
        assertThat(klink.onFix("crew-alpha-1", true)).isNotBlank();
    }

    @Test
    void onFix_notFound_returnsNonBlank() {
        assertThat(klink.onFix("ghost", false)).isNotBlank();
    }

    @Test
    void onLevelUp_level2_containsLevel2Reference() {
        assertThat(klink.onLevelUp(2)).contains("2");
    }

    @Test
    void onLevelUp_level3_containsLevel3Reference() {
        assertThat(klink.onLevelUp(3)).contains("3");
    }

    @Test
    void onGameComplete_containsPlayerNameAndScore() {
        assertThat(klink.onGameComplete("Bob", 500)).contains("Bob").contains("500");
    }

    @Test
    void getIdleComment_returnsNonBlank() {
        assertThat(klink.getIdleComment()).isNotBlank();
    }

    @Test
    void onEscalation_tier1_returnsNonBlank() {
        assertThat(klink.onEscalation(1)).isNotBlank();
    }

    @Test
    void onWrongCommand_returnsNonBlank() {
        assertThat(klink.onWrongCommand()).isNotBlank();
    }
}
