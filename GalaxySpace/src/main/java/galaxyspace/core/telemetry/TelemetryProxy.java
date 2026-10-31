package galaxyspace.core.telemetry;

/** Dedicated servers never load the client implementation. */
public class TelemetryProxy {
    public void receive(TelemetryNetwork.Update update) { }
}
