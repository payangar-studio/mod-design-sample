package com.payangar.designsample.vendor.gui.screen;

import com.payangar.designsample.vendor.config.ConfigSchema;
import com.payangar.designsample.vendor.config.Scope;
import com.payangar.designsample.vendor.config.Setting;
import com.payangar.designsample.vendor.config.SettingsSource;
import com.payangar.designsample.vendor.gui.Texts;
import com.payangar.designsample.vendor.gui.Theme;
import com.payangar.designsample.vendor.gui.widget.Binding;
import com.payangar.designsample.vendor.gui.widget.Label;
import com.payangar.designsample.vendor.gui.widget.ScrollPanel;
import com.payangar.designsample.vendor.gui.widget.Tabs;
import com.payangar.designsample.vendor.gui.widget.TextField;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.slf4j.LoggerFactory;

/**
 * The config screen drawn from the schema: one tab per scope that has settings, a search over
 * the settings' labels, and a row per setting. The settings of a scope whose source is not
 * editable (server settings for a player who is not an operator) are shown read-only. Settings
 * whose file could not be read say that a change will not be saved.
 *
 * <p>A change applies to the values at once; closing the screen saves each scope that changed.
 */
public class GeneratedConfigScreen extends ConfigScreen {
    private final ConfigSchema schema;
    private final Map<Scope, SettingsSource> sources = new LinkedHashMap<>();
    private final Set<Scope> changed = EnumSet.noneOf(Scope.class);
    private final List<Scope> scopes = new ArrayList<>();
    private final List<SettingRow> rows = new ArrayList<>();
    private int tab;
    private String query = "";
    private ScrollPanel panel;
    private Label noMatch;
    private TextField search;
    /** A tab was chosen during an input event: the widgets are rebuilt at the next tick. */
    private boolean rebuild;

    /**
     * @param sources the settings of each scope; a scope of the schema without a source is left
     *     out
     */
    public GeneratedConfigScreen(Component title, Screen parent, ConfigSchema schema, Map<Scope, SettingsSource> sources) {
        super(title, parent);
        this.schema = schema;
        for (Scope scope : Scope.values()) {
            SettingsSource source = sources.get(scope);
            if (source != null && !schema.settings(scope).isEmpty()) {
                this.sources.put(scope, source);
                scopes.add(scope);
            }
        }
    }

    public ConfigSchema schema() {
        return schema;
    }

    /** The scope of the tab shown, or {@code null} when the schema has no setting. */
    public Scope scope() {
        return scopes.isEmpty() ? null : scopes.get(tab);
    }

    /** The rows of the tab shown. */
    public List<SettingRow> rows() {
        return List.copyOf(rows);
    }

    /** The search field. */
    public TextField search() {
        return search;
    }

    /** Shows the tab of {@code scope}, when the screen has one. */
    public void selectScope(Scope scope) {
        int index = scopes.indexOf(scope);
        if (index >= 0 && index != tab) {
            tab = index;
            rebuildWidgets();
        }
    }

    @Override
    protected void initContent(int x, int y, int width, int height) {
        rows.clear();
        String modId = schema.modId();
        int searchWidth = Math.min(160, width / 2);
        search = new TextField(searchWidth, Texts.vendor(modId, "config.search"), Binding.of(() -> query, q -> {
                    query = q;
                    filter();
                }))
                .hint(Texts.vendor(modId, "config.search"));
        search.setPosition(x + width - searchWidth, y + (Theme.ROW_HEIGHT - search.getHeight()) / 2);
        addRenderableWidget(search);
        if (scopes.size() > 1) {
            List<Component> titles = scopes.stream()
                    .map(s -> (Component) Texts.vendor(modId, "config.scope." + s.name().toLowerCase(java.util.Locale.ROOT)))
                    .toList();
            Tabs tabs = new Tabs(Texts.vendor(modId, "config.scopes"), titles, Binding.of(() -> tab, i -> {
                if (i != tab) {
                    tab = i;
                    rebuild = true;
                }
            }));
            tabs.setPosition(x, y);
            addRenderableWidget(tabs);
        }

        int top = y + Theme.ROW_HEIGHT + Theme.PADDING;
        panel = new ScrollPanel(width, y + height - top, Texts.vendor(modId, "config.settings"));
        panel.setPosition(x, top);
        noMatch = Label.dim(panel.innerWidth(), Texts.vendor(modId, "config.no_match"));
        Scope scope = scope();
        if (scope != null) {
            SettingsSource source = sources.get(scope);
            if (!source.editable()) {
                panel.add(Label.dim(panel.innerWidth(), Texts.vendor(modId, "config.read_only")));
            } else if (source.values().fromBrokenFile()) {
                panel.add(Label.dim(panel.innerWidth(), Texts.vendor(modId, "config.broken_file")));
            }
            for (Setting<?> setting : schema.settings(scope)) {
                rows.add(panel.add(row(panel.innerWidth(), setting, source, scope)));
            }
        }
        panel.add(noMatch);
        filter();
        addRenderableWidget(panel);
    }

    private <T> SettingRow row(int width, Setting<T> setting, SettingsSource source, Scope scope) {
        Binding<T> binding = Binding.of(source.values(), setting).onChange(value -> changed.add(scope));
        return new SettingRow(width, schema.modId(), setting, binding, source.editable());
    }

    @Override
    public void tick() {
        super.tick();
        if (rebuild) {
            rebuild = false;
            rebuildWidgets();
        }
    }

    /** Shows the rows that match the search. */
    private void filter() {
        if (panel == null) {
            return;
        }
        boolean any = false;
        for (SettingRow row : rows) {
            row.visible = row.matches(query);
            any |= row.visible;
        }
        noMatch.visible = !any && !rows.isEmpty();
        panel.setScroll(0);
    }

    @Override
    protected void save() {
        for (Scope scope : changed) {
            try {
                sources.get(scope).save();
            } catch (IOException e) {
                LoggerFactory.getLogger(schema.modId()).warn("The {} settings could not be saved: {}", scope, e.toString());
            }
        }
        changed.clear();
    }
}
