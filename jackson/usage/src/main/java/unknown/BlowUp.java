package unknown;

import com.fasterxml.jackson.core.JsonParser;
import com.gitlab.faerytea.mapper.adapters.UnknownPropertyHandler;
import com.gitlab.faerytea.mapper.annotations.Instance;

import java.io.IOException;

public class BlowUp implements UnknownPropertyHandler<JsonParser> {
    private final String sound;

    public BlowUp() {
        this("BOOM!");
    }

    public BlowUp(String sound) {
        this.sound = sound;
    }

    @Override
    public void handle(String name, JsonParser currentInput) throws IOException {
        throw new IllegalStateException(sound + " What is" + name + "?!");
    }

    @Instance("bang")
    public static final BlowUp BANG = new BlowUp("BANG!");
}
