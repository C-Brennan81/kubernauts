package com.kubernauts.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CommandParserNormaliseTest {

    private CommandParser parser;

    @BeforeEach
    void setUp() {
        parser = new CommandParser(
                mock(CommandAdapter.class),
                mock(ScenarioEngine.class),
                mock(KLINKService.class));
    }

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
        "kubectl get pods,                              scan crew-quarters",
        "kubectl get pods -l app=navigation,            scan navigation",
        "kubectl describe pod crew-alpha-1,             inspect crew-alpha-1",
        "kubectl logs nav-system-1,                     read logs nav-system-1",
        "kubectl delete pod crew-alpha-1,               fix crew-alpha-1",
        "kubectl scale deployment life-support --replicas=2, deploy reinforcements life-support --count=2",
        "kubectl rollout undo deployment/crew-quarters, revert mission crew-quarters",
        "kubectl cordon module-beta,                    isolate module-beta",
        "kubectl get nodes,                             status",
    })
    void normalise_kubectlSyntax_mapsToGameCommand(String kubectl, String expected) {
        assertThat(parser.normalise(kubectl.trim())).isEqualTo(expected.trim());
    }

    @Test
    void normalise_nativeCommand_passesThrough() {
        assertThat(parser.normalise("scan crew-quarters")).isEqualTo("scan crew-quarters");
        assertThat(parser.normalise("fix crew-alpha-1")).isEqualTo("fix crew-alpha-1");
        assertThat(parser.normalise("status")).isEqualTo("status");
    }

    @Test
    void normalise_unknownInput_passesThrough() {
        assertThat(parser.normalise("gibberish")).isEqualTo("gibberish");
    }
}
