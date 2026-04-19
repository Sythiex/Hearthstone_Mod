package com.sythiex.hearthstonemod.client;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.sythiex.hearthstonemod.HearthstoneCommon;
import com.sythiex.hearthstonemod.config.HearthstoneConfigData;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

public class HearthstoneConfigScreen extends Screen
{
    private static final int MAX_FORM_WIDTH = 310;
    private static final int MAX_LABEL_WIDTH = 150;
    private static final int BUTTON_WIDTH = 150;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 8;
    private static final int LIST_TOP = 32;
    private static final int LIST_ENTRY_HEIGHT = 24;
    private static final int SCREEN_MARGIN = 16;
    private static final int FOOTER_MARGIN = 8;
    private static final int FOOTER_GAP = 6;
    private static final int TITLE_Y = 15;

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

    private ConfigOptionList optionList;
    private EditBox channelTimeBox;
    private EditBox cooldownBox;
    private CycleButton<Boolean> allowCrossDimensionTravelButton;
    private CycleButton<Boolean> allowRespawnAnchorLinkingButton;
    private CycleButton<Boolean> consumeRespawnAnchorChargeButton;
    private EditBox dimensionBlacklistBox;
    private Button saveButton;
    private Button cancelButton;
    private boolean allowCrossDimensionTravel;
    private boolean allowRespawnAnchorLinking;
    private boolean consumeRespawnAnchorCharge;
    private Component validationMessage;
    private List<FormattedCharSequence> wrappedValidationMessage = List.of();

    public HearthstoneConfigScreen(Screen parentScreen)
    {
        super(TITLE);
        this.parentScreen = parentScreen;
        HearthstoneConfigData currentConfig = HearthstoneCommon.config();
        this.allowCrossDimensionTravel = currentConfig.allowCrossDimensionTravel();
        this.allowRespawnAnchorLinking = currentConfig.allowRespawnAnchorLinking();
        this.consumeRespawnAnchorCharge = currentConfig.consumeRespawnAnchorCharge();
    }

    @Override
    protected void init()
    {
        ConfigDraft draft = this.captureDraft();
        double scrollAmount = this.optionList != null ? this.optionList.getScrollAmount() : 0.0D;

        this.allowCrossDimensionTravel = draft.allowCrossDimensionTravel();
        this.allowRespawnAnchorLinking = draft.allowRespawnAnchorLinking();
        this.consumeRespawnAnchorCharge = draft.consumeRespawnAnchorCharge();

        this.channelTimeBox = this.createNumericBox(CHANNEL_TIME_LABEL, draft.channelTime());
        this.channelTimeBox.setResponder(value -> this.updateValidation());

        this.cooldownBox = this.createNumericBox(COOLDOWN_LABEL, draft.cooldown());
        this.cooldownBox.setResponder(value -> this.updateValidation());

        this.allowCrossDimensionTravelButton = this.createToggle(ALLOW_CROSS_DIMENSION_TRAVEL_LABEL, ALLOW_CROSS_DIMENSION_TRAVEL_HINT, this.allowCrossDimensionTravel,
                value -> this.allowCrossDimensionTravel = value);

        this.allowRespawnAnchorLinkingButton = this.createToggle(ALLOW_RESPAWN_ANCHOR_LINKING_LABEL, ALLOW_RESPAWN_ANCHOR_LINKING_HINT, this.allowRespawnAnchorLinking,
                value -> this.allowRespawnAnchorLinking = value);

        this.consumeRespawnAnchorChargeButton = this.createToggle(CONSUME_RESPAWN_ANCHOR_CHARGE_LABEL, CONSUME_RESPAWN_ANCHOR_CHARGE_HINT, this.consumeRespawnAnchorCharge,
                value -> this.consumeRespawnAnchorCharge = value);

        this.dimensionBlacklistBox = new EditBox(this.font, 0, 0, MAX_FORM_WIDTH, BUTTON_HEIGHT, DIMENSION_BLACKLIST_LABEL);
        this.dimensionBlacklistBox.setMaxLength(512);
        this.dimensionBlacklistBox.setTooltip(Tooltip.create(DIMENSION_BLACKLIST_HINT));
        this.dimensionBlacklistBox.setValue(draft.dimensionBlacklist());
        this.dimensionBlacklistBox.setResponder(value -> this.updateValidation());

        this.clearWidgets();
        this.optionList = this.addRenderableWidget(new ConfigOptionList());
        this.optionList.addConfigEntry(new NumericEntry(CHANNEL_TIME_LABEL, CHANNEL_TIME_HINT, this.channelTimeBox));
        this.optionList.addConfigEntry(new NumericEntry(COOLDOWN_LABEL, COOLDOWN_HINT, this.cooldownBox));
        this.optionList.addConfigEntry(new ToggleEntry(this.allowCrossDimensionTravelButton));
        this.optionList.addConfigEntry(new ToggleEntry(this.allowRespawnAnchorLinkingButton));
        this.optionList.addConfigEntry(new ToggleEntry(this.consumeRespawnAnchorChargeButton));
        this.optionList.addConfigEntry(new LabelEntry(DIMENSION_BLACKLIST_LABEL, DIMENSION_BLACKLIST_HINT));
        this.optionList.addConfigEntry(new TextFieldEntry(this.dimensionBlacklistBox));
        this.optionList.setScrollAmount(scrollAmount);

        this.saveButton = this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.saveAndClose()).bounds(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        this.cancelButton = this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> this.onClose()).bounds(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        this.updateValidation();
        this.repositionElements();
        this.setInitialFocus(this.channelTimeBox);
    }

    @Override
    protected void repositionElements()
    {
        this.refreshValidationLines();

        if (this.optionList != null)
        {
            int listBottom = this.height - this.getFooterHeight() - FOOTER_GAP;
            int listHeight = Math.max(LIST_ENTRY_HEIGHT, listBottom - LIST_TOP);
            this.optionList.updateSizeAndPosition(this.width, listHeight, LIST_TOP);
        }

        int buttonWidth = this.getButtonWidth();
        int buttonsLeft = (this.width - (buttonWidth * 2 + BUTTON_SPACING)) / 2;
        int buttonY = this.height - FOOTER_MARGIN - BUTTON_HEIGHT;

        if (this.saveButton != null)
        {
            this.saveButton.setWidth(buttonWidth);
            this.saveButton.setX(buttonsLeft);
            this.saveButton.setY(buttonY);
        }

        if (this.cancelButton != null)
        {
            this.cancelButton.setWidth(buttonWidth);
            this.cancelButton.setX(buttonsLeft + buttonWidth + BUTTON_SPACING);
            this.cancelButton.setY(buttonY);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
    {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, TITLE_Y, 0xFFFFFF);
        this.renderValidationMessage(guiGraphics);
    }

    private void renderValidationMessage(GuiGraphics guiGraphics)
    {
        if (this.wrappedValidationMessage.isEmpty() || this.saveButton == null)
        {
            return;
        }

        int y = this.saveButton.getY() - FOOTER_GAP - this.wrappedValidationMessage.size() * this.font.lineHeight;
        for (FormattedCharSequence line : this.wrappedValidationMessage)
        {
            guiGraphics.drawCenteredString(this.font, line, this.width / 2, y, 0xFF5555);
            y += this.font.lineHeight;
        }
    }

    @Override
    public void onClose()
    {
        if (this.minecraft != null)
        {
            this.minecraft.setScreen(this.parentScreen);
        }
    }

    private void saveAndClose()
    {
        Integer channelTime = this.parseChannelTime();
        Integer cooldown = this.parseCooldown();
        List<String> dimensionBlacklist = this.parseDimensionBlacklist();
        if (channelTime == null || cooldown == null || dimensionBlacklist == null)
        {
            this.updateValidation();
            return;
        }

        HearthstoneConfigData updatedConfig = new HearthstoneConfigData(channelTime, cooldown, this.allowCrossDimensionTravel, this.allowRespawnAnchorLinking,
                this.consumeRespawnAnchorCharge, dimensionBlacklist);

        try
        {
            HearthstoneCommon.configStore().save(updatedConfig);
            this.onClose();
        } catch (IOException exception)
        {
            this.validationMessage = SAVE_ERROR;
            this.refreshValidationLines();
            if (this.saveButton != null)
            {
                this.saveButton.active = true;
            }
            this.repositionElements();
        }
    }

    private void updateValidation()
    {
        this.validationMessage = this.getValidationMessage();
        this.refreshValidationLines();
        if (this.saveButton != null)
        {
            this.saveButton.active = this.validationMessage == null;
        }
        if (this.optionList != null && this.cancelButton != null)
        {
            this.repositionElements();
        }
    }

    private Component getValidationMessage()
    {
        if (this.parseChannelTime() == null)
        {
            return CHANNEL_TIME_ERROR;
        }

        if (this.parseCooldown() == null)
        {
            return COOLDOWN_ERROR;
        }

        if (this.parseDimensionBlacklist() == null)
        {
            return DIMENSION_BLACKLIST_ERROR;
        }

        return null;
    }

    private Integer parseChannelTime()
    {
        return this.parseInt(this.channelTimeBox, 1);
    }

    private Integer parseCooldown()
    {
        return this.parseInt(this.cooldownBox, 0);
    }

    private List<String> parseDimensionBlacklist()
    {
        String rawValue = this.dimensionBlacklistBox.getValue().trim();
        if (rawValue.isEmpty())
        {
            return List.of();
        }

        Set<String> parsedDimensions = new LinkedHashSet<>();
        for (String rawDimensionId : rawValue.split(","))
        {
            String dimensionId = rawDimensionId.trim();
            if (dimensionId.isEmpty())
            {
                continue;
            }

            if (ResourceLocation.tryParse(dimensionId) == null)
            {
                return null;
            }

            parsedDimensions.add(dimensionId);
        }

        return List.copyOf(parsedDimensions);
    }

    private Integer parseInt(EditBox editBox, int minimumValue)
    {
        try
        {
            int parsedValue = Integer.parseInt(editBox.getValue());
            return parsedValue >= minimumValue ? parsedValue : null;
        } catch (NumberFormatException exception)
        {
            return null;
        }
    }

    private EditBox createNumericBox(Component label, String value)
    {
        EditBox editBox = new EditBox(this.font, 0, 0, MAX_FORM_WIDTH, BUTTON_HEIGHT, label);
        editBox.setMaxLength(10);
        editBox.setValue(value);
        return editBox;
    }

    private StringWidget createFieldLabel(Component label, Component hint, int width)
    {
        StringWidget stringWidget = new StringWidget(width, BUTTON_HEIGHT, label, this.font).alignLeft().setColor(0xA0A0A0);
        stringWidget.setTooltip(Tooltip.create(hint));
        return stringWidget;
    }

    private CycleButton<Boolean> createToggle(Component label, Component hint, boolean value, java.util.function.Consumer<Boolean> consumer)
    {
        CycleButton<Boolean> cycleButton = CycleButton.onOffBuilder(value).create(0, 0, MAX_FORM_WIDTH, BUTTON_HEIGHT, label, (button, newValue) -> consumer.accept(newValue));
        cycleButton.setTooltip(Tooltip.create(hint));
        return cycleButton;
    }

    private int getFormWidth()
    {
        return Math.max(180, Math.min(MAX_FORM_WIDTH, this.width - SCREEN_MARGIN * 2));
    }

    private int getLabelWidth(int formWidth)
    {
        return Math.min(MAX_LABEL_WIDTH, Math.max(90, (formWidth - 10) / 2));
    }

    private int getControlWidth(int formWidth)
    {
        return Math.max(80, formWidth - this.getLabelWidth(formWidth) - 10);
    }

    private int getButtonWidth()
    {
        return Math.min(BUTTON_WIDTH, Math.max(80, (this.width - SCREEN_MARGIN * 2 - BUTTON_SPACING) / 2));
    }

    private int getValidationWrapWidth()
    {
        return Math.max(120, this.width - SCREEN_MARGIN * 2);
    }

    private void refreshValidationLines()
    {
        this.wrappedValidationMessage = this.validationMessage == null ? List.of() : this.font.split(this.validationMessage, this.getValidationWrapWidth());
    }

    private int getFooterHeight()
    {
        int validationHeight = this.wrappedValidationMessage.isEmpty() ? 0 : this.wrappedValidationMessage.size() * this.font.lineHeight + FOOTER_GAP;
        return FOOTER_MARGIN * 2 + BUTTON_HEIGHT + validationHeight;
    }

    private final class ConfigOptionList extends ContainerObjectSelectionList<ConfigEntry>
    {
        private ConfigOptionList()
        {
            super(HearthstoneConfigScreen.this.minecraft, HearthstoneConfigScreen.this.width, HearthstoneConfigScreen.this.height, LIST_TOP, LIST_ENTRY_HEIGHT);
            this.centerListVertically = false;
        }

        private void addConfigEntry(ConfigEntry entry)
        {
            this.addEntry(entry);
        }

        @Override
        public int getRowWidth()
        {
            return HearthstoneConfigScreen.this.getFormWidth();
        }

        @Override
        protected int getScrollbarPosition()
        {
            return this.getRowRight() + 6;
        }
    }

    private abstract static class ConfigEntry extends ContainerObjectSelectionList.Entry<ConfigEntry>
    {}

    private final class NumericEntry extends ConfigEntry
    {
        private final StringWidget label;
        private final EditBox editBox;

        private NumericEntry(Component labelText, Component hint, EditBox editBox)
        {
            this.label = HearthstoneConfigScreen.this.createFieldLabel(labelText, hint, MAX_LABEL_WIDTH);
            this.editBox = editBox;
            this.editBox.setTooltip(Tooltip.create(hint));
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick)
        {
            int formWidth = HearthstoneConfigScreen.this.getFormWidth();
            int labelWidth = HearthstoneConfigScreen.this.getLabelWidth(formWidth);
            int controlWidth = HearthstoneConfigScreen.this.getControlWidth(formWidth);
            int y = top + (height - BUTTON_HEIGHT) / 2;

            this.label.setWidth(labelWidth);
            this.label.setX(left);
            this.label.setY(y);

            this.editBox.setWidth(controlWidth);
            this.editBox.setX(left + labelWidth + 10);
            this.editBox.setY(y);

            this.label.render(guiGraphics, mouseX, mouseY, partialTick);
            this.editBox.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        @Override
        public List<? extends GuiEventListener> children()
        {
            return List.of(this.label, this.editBox);
        }

        @Override
        public List<? extends NarratableEntry> narratables()
        {
            return List.of(this.editBox);
        }
    }

    private final class ToggleEntry extends ConfigEntry
    {
        private final CycleButton<Boolean> button;

        private ToggleEntry(CycleButton<Boolean> button)
        {
            this.button = button;
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick)
        {
            int y = top + (height - BUTTON_HEIGHT) / 2;
            this.button.setWidth(HearthstoneConfigScreen.this.getFormWidth());
            this.button.setX(left);
            this.button.setY(y);
            this.button.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        @Override
        public List<? extends GuiEventListener> children()
        {
            return List.of(this.button);
        }

        @Override
        public List<? extends NarratableEntry> narratables()
        {
            return List.of(this.button);
        }
    }

    private final class LabelEntry extends ConfigEntry
    {
        private final StringWidget label;

        private LabelEntry(Component labelText, Component hint)
        {
            this.label = HearthstoneConfigScreen.this.createFieldLabel(labelText, hint, MAX_FORM_WIDTH);
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick)
        {
            int y = top + (height - BUTTON_HEIGHT) / 2;
            this.label.setWidth(HearthstoneConfigScreen.this.getFormWidth());
            this.label.setX(left);
            this.label.setY(y);
            this.label.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        @Override
        public List<? extends GuiEventListener> children()
        {
            return List.of(this.label);
        }

        @Override
        public List<? extends NarratableEntry> narratables()
        {
            return List.of(this.label);
        }
    }

    private final class TextFieldEntry extends ConfigEntry
    {
        private final EditBox editBox;

        private TextFieldEntry(EditBox editBox)
        {
            this.editBox = editBox;
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick)
        {
            int y = top + (height - BUTTON_HEIGHT) / 2;
            this.editBox.setWidth(HearthstoneConfigScreen.this.getFormWidth());
            this.editBox.setX(left);
            this.editBox.setY(y);
            this.editBox.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        @Override
        public List<? extends GuiEventListener> children()
        {
            return List.of(this.editBox);
        }

        @Override
        public List<? extends NarratableEntry> narratables()
        {
            return List.of(this.editBox);
        }
    }

    private ConfigDraft captureDraft()
    {
        HearthstoneConfigData currentConfig = HearthstoneCommon.config();
        return new ConfigDraft(this.channelTimeBox != null ? this.channelTimeBox.getValue() : Integer.toString(currentConfig.channelTime()),
                this.cooldownBox != null ? this.cooldownBox.getValue() : Integer.toString(currentConfig.cooldown()), this.allowCrossDimensionTravel,
                this.allowRespawnAnchorLinking, this.consumeRespawnAnchorCharge,
                this.dimensionBlacklistBox != null ? this.dimensionBlacklistBox.getValue() : String.join(", ", currentConfig.dimensionBlacklist()));
    }

    private record ConfigDraft(String channelTime, String cooldown, boolean allowCrossDimensionTravel, boolean allowRespawnAnchorLinking, boolean consumeRespawnAnchorCharge,
            String dimensionBlacklist)
    {}
}
