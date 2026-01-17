package com.sl.party.hud;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

/**
 * Test HUD for experimenting with CustomUIHud system
 */
public class TestHud extends CustomUIHud {

    // Different test approaches
    private static final String[] TEST_CONFIGS = {
        "root",           // 0: append without selector (to root)
        "simple-div",     // 1: simple div without style
        "span",           // 2: span element
        "p",              // 3: p element
        "minimal-style",  // 4: div with minimal style
        "#hud",           // 5: selector #hud
        "#root",          // 6: selector #root
        "body",           // 7: selector body
        "#app",           // 8: selector #app
        ".hud",           // 9: class selector
        "#custom-hud"     // 10: selector #custom-hud
    };

    private int selectorIndex = 0;
    private String testMessage = "TEST HUD";

    public TestHud(PlayerRef playerRef) {
        super(playerRef);
    }

    public void setSelectorIndex(int index) {
        this.selectorIndex = index % TEST_CONFIGS.length;
    }

    public void setTestMessage(String message) {
        this.testMessage = message;
    }

    public String getCurrentSelector() {
        return TEST_CONFIGS[selectorIndex];
    }

    @Override
    protected void build(UICommandBuilder builder) {
        String config = TEST_CONFIGS[selectorIndex];

        switch (config) {
            case "root":
                // Try empty - just clear
                builder.clear("#sl-party-test");
                break;
            case "simple-div":
                // Try set on known compass element
                builder.set("#compass-title", "PARTY TEST");
                break;
            case "span":
                // Try set on chat
                builder.set("#chat-title", "PARTY");
                break;
            case "p":
                // Try appendInline instead
                builder.appendInline("body", "TEST");
                break;
            case "minimal-style":
                // Try plain text append
                builder.append("body", "TEST");
                break;
            default:
                // Use config as CSS selector with plain text
                builder.set(config, "TEST");
                break;
        }
    }

    /**
     * Update existing element without rebuild
     */
    public void updateText(String newText) {
        this.testMessage = newText;
        UICommandBuilder builder = new UICommandBuilder();
        // Try to update text using set
        builder.set("#sl-party-test", newText + " [Updated]");
        update(false, builder);
    }

    /**
     * Try clearing and rebuilding
     */
    public void rebuild() {
        UICommandBuilder builder = new UICommandBuilder();
        builder.clear("#sl-party-test");
        update(true, builder);
    }
}
