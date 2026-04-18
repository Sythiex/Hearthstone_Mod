package com.sythiex.hearthstonemod.client;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.sythiex.hearthstonemod.HearthstoneCommon;
import com.sythiex.hearthstonemod.config.HearthstoneConfigData;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class HearthstoneConfigScreen extends Screen {
    private static final int LIST_TOP = 32;
    private static final int LIST_BOTTOM = 64;
    private static final int ROW_HEIGHT = 38;
    private static final int BUTTON_WIDTH = 150;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 5;

    private static final Component TITLE = Component.translatable("text.hearthstonemod.config.title");
    private static final Component CHANNEL_TIME_LABEL = Component.translatable("config.hearthstone.channel_time");
    private static final Component CHANNEL_TIME_HINT = Component.translatable("text.hearthstonemod.config.channel_time_hint");
    private static final Component CHANNEL_TIME_ERROR = Component.translatable("text.hearthstonemod.config.channel_time_error");
    private static final Component COOLDOWN_LABEL = Component.translatable("config.hearthstone.cooldown");
    private static final Component COOLDOWN_HINT = Component.translatable("text.hearthstonemod.config.cooldown_hint");
    private static final Component COOLDOWN_ERROR = Component.translatable("text.hearthstonemod.config.cooldown_error");
    private static final Component ALLOW_CROSS_DIMENSION_TRAVEL_LABEL = Component.translatable("config.hearthstone.allow_cross_dimension_travel");
    private static final Component ALLOW_CROSS_DIMENSION_TRAVEL_HINT = Component.translatable("text.hearthstonemod.config.allow_cross_dimension_travel_hint");
    private static final Component ALLOW_RESPAWN_ANCHOR_LINKING_LABEL = Component.translatable("config.hearthstone.allow_respawn_anchor_linking");
    private static final Component ALLOW_RESPAWN_ANCHOR_LINKING_HINT = Component.translatable("text.hearthstonemod.config.allow_respawn_anchor_linking_hint");
    private static final Component CONSUME_RESPAWN_ANCHOR_CHARGE_LABEL = Component.translatable("config.hearthstone.consume_respawn_anchor_charge");
    private static final Component CONSUME_RESPAWN_ANCHOR_CHARGE_HINT = Component.translatable("text.hearthstonemod.config.consume_respawn_anchor_charge_hint");
    private static final Component DIMENSION_BLACKLIST_LABEL = Component.translatable("config.hearthstone.dimBlacklist");
    private static final Component DIMENSION_BLACKLIST_HINT = Component.translatable("text.hearthstonemod.config.dimension_blacklist_hint");
    private static final Component DIMENSION_BLACKLIST_ERROR = Component.translatable("text.hearthstonemod.config.dimension_blacklist_error");
    private static final Component SAVE_ERROR = Component.translatable("text.hearthstonemod.config.save_error");

    private final Screen parentScreen;

    private HearthstoneOptionList optionList;
    private EditBox channelTimeBox;
    private EditBox cooldownBox;
    private CycleButton<Boolean> allowCrossDimensionTravelButton;
    private CycleButton<Boolean> allowRespawnAnchorLinkingButton;
    private CycleButton<Boolean> consumeRespawnAnchorChargeButton;
    private EditBox dimensionBlacklistBox;
    private Button saveButton;
    private boolean allowCrossDimensionTravel;
    private boolean allowRespawnAnchorLinking;
    private boolean consumeRespawnAnchorCharge;
    private Component validationMessage;
    private Component hoveredTooltip;

    public HearthstoneConfigScreen(Screen parentScreen) {
        super(TITLE);
        this.parentScreen = parentScreen;
        HearthstoneConfigData currentConfig = HearthstoneCommon.config();
        this.allowCrossDimensionTravel = currentConfig.allowCrossDimensionTravel();
        this.allowRespawnAnchorLinking = currentConfig.allowRespawnAnchorLinking();
        this.consumeRespawnAnchorCharge = currentConfig.consumeRespawnAnchorCharge();
    }

    @Override
    protected void init() {
        HearthstoneConfigData currentConfig = HearthstoneCommon.config();
        int rowWidth = this.getOptionWidth();
        int controlWidth = this.getControlWidth(rowWidth);

        this.channelTimeBox = new EditBox(this.font, 0, 0, controlWidth, 20, CHANNEL_TIME_LABEL);
        this.channelTimeBox.setMaxLength(10);
        this.channelTimeBox.setValue(Integer.toString(currentConfig.channelTime()));
        this.channelTimeBox.setResponder(value -> this.updateValidation());

        this.cooldownBox = new EditBox(this.font, 0, 0, controlWidth, 20, COOLDOWN_LABEL);
        this.cooldownBox.setMaxLength(10);
        this.cooldownBox.setValue(Integer.toString(currentConfig.cooldown()));
        this.cooldownBox.setResponder(value -> this.updateValidation());

        this.allowCrossDimensionTravelButton = CycleButton.onOffBuilder(this.allowCrossDimensionTravel)
                .displayOnlyValue()
                .create(0, 0, controlWidth, 20, ALLOW_CROSS_DIMENSION_TRAVEL_LABEL, (button, value) -> this.allowCrossDimensionTravel = value);

        this.allowRespawnAnchorLinkingButton = CycleButton.onOffBuilder(this.allowRespawnAnchorLinking)
                .displayOnlyValue()
                .create(0, 0, controlWidth, 20, ALLOW_RESPAWN_ANCHOR_LINKING_LABEL, (button, value) -> this.allowRespawnAnchorLinking = value);

        this.consumeRespawnAnchorChargeButton = CycleButton.onOffBuilder(this.consumeRespawnAnchorCharge)
                .displayOnlyValue()
                .create(0, 0, controlWidth, 20, CONSUME_RESPAWN_ANCHOR_CHARGE_LABEL, (button, value) -> this.consumeRespawnAnchorCharge = value);

        this.dimensionBlacklistBox = new EditBox(this.font, 0, 0, controlWidth, 20, DIMENSION_BLACKLIST_LABEL);
        this.dimensionBlacklistBox.setMaxLength(512);
        this.dimensionBlacklistBox.setValue(String.join(", ", currentConfig.dimensionBlacklist()));
        this.dimensionBlacklistBox.setResponder(value -> this.updateValidation());

        this.optionList = new HearthstoneOptionList(this.minecraft, this.width, this.height, LIST_TOP, this.height - LIST_BOTTOM, ROW_HEIGHT, rowWidth);
        this.optionList.addOption(new OptionEntry(CHANNEL_TIME_LABEL, CHANNEL_TIME_HINT, this.channelTimeBox));
        this.optionList.addOption(new OptionEntry(COOLDOWN_LABEL, COOLDOWN_HINT, this.cooldownBox));
        this.optionList.addOption(new OptionEntry(ALLOW_CROSS_DIMENSION_TRAVEL_LABEL, ALLOW_CROSS_DIMENSION_TRAVEL_HINT, this.allowCrossDimensionTravelButton));
        this.optionList.addOption(new OptionEntry(ALLOW_RESPAWN_ANCHOR_LINKING_LABEL, ALLOW_RESPAWN_ANCHOR_LINKING_HINT, this.allowRespawnAnchorLinkingButton));
        this.optionList.addOption(new OptionEntry(CONSUME_RESPAWN_ANCHOR_CHARGE_LABEL, CONSUME_RESPAWN_ANCHOR_CHARGE_HINT, this.consumeRespawnAnchorChargeButton));
        this.optionList.addOption(new OptionEntry(DIMENSION_BLACKLIST_LABEL, DIMENSION_BLACKLIST_HINT, this.dimensionBlacklistBox));
        this.addWidget(this.optionList);

        int buttonWidth = this.getButtonWidth();
        int totalButtonWidth = buttonWidth * 2 + BUTTON_GAP;
        int buttonLeft = this.width / 2 - totalButtonWidth / 2;
        int buttonY = this.height - 27;

        this.saveButton = this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.saveAndClose()).bounds(buttonLeft, buttonY, buttonWidth, BUTTON_HEIGHT).build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> this.onClose()).bounds(buttonLeft + buttonWidth + BUTTON_GAP, buttonY, buttonWidth, BUTTON_HEIGHT).build());

        this.setInitialFocus(this.channelTimeBox);
        this.updateValidation();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.hoveredTooltip = null;
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        this.optionList.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);

        if (this.validationMessage != null) {
            guiGraphics.drawCenteredString(this.font, this.validationMessage, this.width / 2, this.height - 45, 0xFF5555);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        if (this.hoveredTooltip != null) {
            guiGraphics.renderTooltip(this.font, this.hoveredTooltip, mouseX, mouseY);
        }
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        String channelTimeValue = this.channelTimeBox != null ? this.channelTimeBox.getValue() : Integer.toString(HearthstoneCommon.config().channelTime());
        String cooldownValue = this.cooldownBox != null ? this.cooldownBox.getValue() : Integer.toString(HearthstoneCommon.config().cooldown());
        boolean allowCrossDimensionTravelValue = this.allowCrossDimensionTravel;
        boolean allowRespawnAnchorLinkingValue = this.allowRespawnAnchorLinking;
        boolean consumeRespawnAnchorChargeValue = this.consumeRespawnAnchorCharge;
        String dimensionBlacklistValue = this.dimensionBlacklistBox != null ? this.dimensionBlacklistBox.getValue() : String.join(", ", HearthstoneCommon.config().dimensionBlacklist());
        double scrollAmount = this.optionList != null ? this.optionList.getScrollAmount() : 0.0D;

        super.resize(minecraft, width, height);

        this.channelTimeBox.setValue(channelTimeValue);
        this.cooldownBox.setValue(cooldownValue);
        this.allowCrossDimensionTravel = allowCrossDimensionTravelValue;
        this.allowCrossDimensionTravelButton.setValue(this.allowCrossDimensionTravel);
        this.allowRespawnAnchorLinking = allowRespawnAnchorLinkingValue;
        this.allowRespawnAnchorLinkingButton.setValue(this.allowRespawnAnchorLinking);
        this.consumeRespawnAnchorCharge = consumeRespawnAnchorChargeValue;
        this.consumeRespawnAnchorChargeButton.setValue(this.consumeRespawnAnchorCharge);
        this.dimensionBlacklistBox.setValue(dimensionBlacklistValue);
        this.optionList.setScrollAmount(scrollAmount);
        this.updateValidation();
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parentScreen);
        }
    }

    private void saveAndClose() {
        Integer channelTime = this.parseChannelTime();
        Integer cooldown = this.parseCooldown();
        List<String> dimensionBlacklist = this.parseDimensionBlacklist();
        if (channelTime == null || cooldown == null || dimensionBlacklist == null) {
            this.updateValidation();
            return;
        }

        HearthstoneConfigData updatedConfig = new HearthstoneConfigData(
                channelTime,
                cooldown,
                this.allowCrossDimensionTravel,
                this.allowRespawnAnchorLinking,
                this.consumeRespawnAnchorCharge,
                dimensionBlacklist
        );

        try {
            HearthstoneCommon.configStore().save(updatedConfig);
            this.onClose();
        } catch (IOException exception) {
            this.validationMessage = SAVE_ERROR;
            if (this.saveButton != null) {
                this.saveButton.active = true;
            }
        }
    }

    private void updateValidation() {
        this.validationMessage = this.getValidationMessage();
        if (this.saveButton != null) {
            this.saveButton.active = this.validationMessage == null;
        }
    }

    private Component getValidationMessage() {
        if (this.parseChannelTime() == null) {
            return CHANNEL_TIME_ERROR;
        }

        if (this.parseCooldown() == null) {
            return COOLDOWN_ERROR;
        }

        if (this.parseDimensionBlacklist() == null) {
            return DIMENSION_BLACKLIST_ERROR;
        }

        return null;
    }

    private Integer parseChannelTime() {
        return this.parseInt(this.channelTimeBox, 1);
    }

    private Integer parseCooldown() {
        return this.parseInt(this.cooldownBox, 0);
    }

    private List<String> parseDimensionBlacklist() {
        String rawValue = this.dimensionBlacklistBox.getValue().trim();
        if (rawValue.isEmpty()) {
            return List.of();
        }

        Set<String> parsedDimensions = new LinkedHashSet<>();
        for (String rawDimensionId : rawValue.split(",")) {
            String dimensionId = rawDimensionId.trim();
            if (dimensionId.isEmpty()) {
                continue;
            }

            if (ResourceLocation.tryParse(dimensionId) == null) {
                return null;
            }

            parsedDimensions.add(dimensionId);
        }

        return List.copyOf(parsedDimensions);
    }

    private Integer parseInt(EditBox editBox, int minimumValue) {
        try {
            int parsedValue = Integer.parseInt(editBox.getValue());
            return parsedValue >= minimumValue ? parsedValue : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private void setHoveredTooltip(Component tooltip) {
        if (this.hoveredTooltip == null) {
            this.hoveredTooltip = tooltip;
        }
    }

    private int getOptionWidth() {
        return Math.max(80, Math.min(310, this.width - 40));
    }

    private int getControlWidth(int rowWidth) {
        return Math.max(110, Math.min(170, rowWidth / 2));
    }

    private int getButtonWidth() {
        return Math.max(80, Math.min(BUTTON_WIDTH, (this.width - BUTTON_GAP - 20) / 2));
    }

    private class HearthstoneOptionList extends ContainerObjectSelectionList<HearthstoneOptionList.Entry> {
        private final int rowWidth;

        HearthstoneOptionList(Minecraft minecraft, int width, int height, int top, int bottom, int itemHeight, int rowWidth) {
            super(minecraft, width, height, top, bottom);
            this.rowWidth = rowWidth;
            this.centerListVertically = false;
        }

        @Override
        public int getRowWidth() {
            return this.rowWidth;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.width / 2 + this.rowWidth / 2 + 4;
        }

        void addOption(HearthstoneOptionList.Entry entry) {
            this.addEntry(entry);
        }

        private abstract static class Entry extends ContainerObjectSelectionList.Entry<HearthstoneOptionList.Entry> {
        }
    }

    private class OptionEntry extends HearthstoneOptionList.Entry {
        private final Component label;
        private final Component hint;
        private final AbstractWidget widget;
        private final List<AbstractWidget> children;

        OptionEntry(Component label, Component hint, AbstractWidget widget) {
            this.label = label;
            this.hint = hint;
            this.widget = widget;
            this.children = List.of(widget);
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int rowWidth, int rowHeight, int mouseX, int mouseY, boolean hovered, float partialTick) {
            int labelX = left;
            int labelY = top + (rowHeight - HearthstoneConfigScreen.this.font.lineHeight) / 2;
            int widgetX = left + rowWidth - this.widget.getWidth();
            int widgetY = top + (rowHeight - this.widget.getHeight()) / 2;

            guiGraphics.drawString(HearthstoneConfigScreen.this.font, this.label, labelX, labelY, 0xA0A0A0, false);

            this.widget.setX(widgetX);
            this.widget.setY(widgetY);
            this.widget.render(guiGraphics, mouseX, mouseY, partialTick);

            if (mouseX >= labelX && mouseX < labelX + HearthstoneConfigScreen.this.font.width(this.label) && mouseY >= labelY && mouseY < labelY + HearthstoneConfigScreen.this.font.lineHeight) {
                HearthstoneConfigScreen.this.setHoveredTooltip(this.hint);
            }
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return this.children;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return this.children;
        }
    }
}
