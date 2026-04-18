package com.sythiex.hearthstonemod.client;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.sythiex.hearthstonemod.HearthstoneCommon;
import com.sythiex.hearthstonemod.config.HearthstoneConfigData;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class HearthstoneConfigScreen extends Screen {
    private static final int FORM_WIDTH = 310;
    private static final int LABEL_WIDTH = 150;
    private static final int CONTROL_WIDTH = FORM_WIDTH - LABEL_WIDTH - 10;
    private static final int BUTTON_WIDTH = 150;
    private static final int BUTTON_HEIGHT = 20;
    private static final int CONTENT_SPACING = 6;

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

    private HeaderAndFooterLayout layout;
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
        ConfigDraft draft = this.captureDraft();
        this.layout = new HeaderAndFooterLayout(this);
        this.layout.addTitleHeader(this.title, this.font);

        this.allowCrossDimensionTravel = draft.allowCrossDimensionTravel();
        this.allowRespawnAnchorLinking = draft.allowRespawnAnchorLinking();
        this.consumeRespawnAnchorCharge = draft.consumeRespawnAnchorCharge();

        LinearLayout content = this.layout.addToContents(LinearLayout.vertical().spacing(CONTENT_SPACING), settings -> settings.alignHorizontallyCenter());

        this.channelTimeBox = this.createNumericBox(CHANNEL_TIME_LABEL, draft.channelTime());
        this.channelTimeBox.setResponder(value -> this.updateValidation());
        content.addChild(this.createLabeledRow(CHANNEL_TIME_LABEL, CHANNEL_TIME_HINT, this.channelTimeBox));

        this.cooldownBox = this.createNumericBox(COOLDOWN_LABEL, draft.cooldown());
        this.cooldownBox.setResponder(value -> this.updateValidation());
        content.addChild(this.createLabeledRow(COOLDOWN_LABEL, COOLDOWN_HINT, this.cooldownBox));

        this.allowCrossDimensionTravelButton = this.createToggle(
                ALLOW_CROSS_DIMENSION_TRAVEL_LABEL,
                ALLOW_CROSS_DIMENSION_TRAVEL_HINT,
                this.allowCrossDimensionTravel,
                value -> this.allowCrossDimensionTravel = value
        );
        content.addChild(this.allowCrossDimensionTravelButton);

        this.allowRespawnAnchorLinkingButton = this.createToggle(
                ALLOW_RESPAWN_ANCHOR_LINKING_LABEL,
                ALLOW_RESPAWN_ANCHOR_LINKING_HINT,
                this.allowRespawnAnchorLinking,
                value -> this.allowRespawnAnchorLinking = value
        );
        content.addChild(this.allowRespawnAnchorLinkingButton);

        this.consumeRespawnAnchorChargeButton = this.createToggle(
                CONSUME_RESPAWN_ANCHOR_CHARGE_LABEL,
                CONSUME_RESPAWN_ANCHOR_CHARGE_HINT,
                this.consumeRespawnAnchorCharge,
                value -> this.consumeRespawnAnchorCharge = value
        );
        content.addChild(this.consumeRespawnAnchorChargeButton);

        LinearLayout blacklistSection = LinearLayout.vertical().spacing(4);
        blacklistSection.addChild(this.createSectionLabel(DIMENSION_BLACKLIST_LABEL, DIMENSION_BLACKLIST_HINT));
        this.dimensionBlacklistBox = new EditBox(this.font, 0, 0, FORM_WIDTH, BUTTON_HEIGHT, DIMENSION_BLACKLIST_LABEL);
        this.dimensionBlacklistBox.setMaxLength(512);
        this.dimensionBlacklistBox.setTooltip(Tooltip.create(DIMENSION_BLACKLIST_HINT));
        this.dimensionBlacklistBox.setValue(draft.dimensionBlacklist());
        this.dimensionBlacklistBox.setResponder(value -> this.updateValidation());
        blacklistSection.addChild(this.dimensionBlacklistBox);
        content.addChild(blacklistSection);

        LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(8), settings -> settings.alignHorizontallyCenter());
        this.saveButton = footer.addChild(Button.builder(CommonComponents.GUI_DONE, button -> this.saveAndClose()).width(BUTTON_WIDTH).build());
        footer.addChild(Button.builder(CommonComponents.GUI_CANCEL, button -> this.onClose()).width(BUTTON_WIDTH).build());

        this.layout.visitWidgets(this::addRenderableWidget);
        this.repositionElements();
        this.setInitialFocus(this.channelTimeBox);
        this.updateValidation();
    }

    @Override
    protected void repositionElements() {
        if (this.layout != null) {
            this.layout.arrangeElements();
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        if (this.validationMessage != null && this.layout != null) {
            int messageY = this.height - this.layout.getFooterHeight() - this.font.lineHeight - 6;
            guiGraphics.drawCenteredString(this.font, this.validationMessage, this.width / 2, messageY, 0xFF5555);
        }
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

    private EditBox createNumericBox(Component label, String value) {
        EditBox editBox = new EditBox(this.font, 0, 0, CONTROL_WIDTH, BUTTON_HEIGHT, label);
        editBox.setMaxLength(10);
        editBox.setValue(value);
        return editBox;
    }

    private LinearLayout createLabeledRow(Component label, Component hint, EditBox editBox) {
        LinearLayout row = LinearLayout.horizontal().spacing(10);
        row.addChild(this.createFieldLabel(label, hint, LABEL_WIDTH));
        editBox.setTooltip(Tooltip.create(hint));
        row.addChild(editBox);
        return row;
    }

    private StringWidget createFieldLabel(Component label, Component hint, int width) {
        StringWidget stringWidget = new StringWidget(width, BUTTON_HEIGHT, label, this.font).alignLeft().setColor(0xA0A0A0);
        stringWidget.setTooltip(Tooltip.create(hint));
        return stringWidget;
    }

    private StringWidget createSectionLabel(Component label, Component hint) {
        return this.createFieldLabel(label, hint, FORM_WIDTH);
    }

    private CycleButton<Boolean> createToggle(Component label, Component hint, boolean value, java.util.function.Consumer<Boolean> consumer) {
        CycleButton<Boolean> cycleButton = CycleButton.onOffBuilder(value)
                .create(0, 0, FORM_WIDTH, BUTTON_HEIGHT, label, (button, newValue) -> consumer.accept(newValue));
        cycleButton.setTooltip(Tooltip.create(hint));
        return cycleButton;
    }

    private ConfigDraft captureDraft() {
        HearthstoneConfigData currentConfig = HearthstoneCommon.config();
        return new ConfigDraft(
                this.channelTimeBox != null ? this.channelTimeBox.getValue() : Integer.toString(currentConfig.channelTime()),
                this.cooldownBox != null ? this.cooldownBox.getValue() : Integer.toString(currentConfig.cooldown()),
                this.allowCrossDimensionTravel,
                this.allowRespawnAnchorLinking,
                this.consumeRespawnAnchorCharge,
                this.dimensionBlacklistBox != null ? this.dimensionBlacklistBox.getValue() : String.join(", ", currentConfig.dimensionBlacklist())
        );
    }

    private record ConfigDraft(
            String channelTime,
            String cooldown,
            boolean allowCrossDimensionTravel,
            boolean allowRespawnAnchorLinking,
            boolean consumeRespawnAnchorCharge,
            String dimensionBlacklist
    ) {
    }
}
