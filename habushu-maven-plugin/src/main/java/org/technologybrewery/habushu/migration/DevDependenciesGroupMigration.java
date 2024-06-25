package org.technologybrewery.habushu.migration;

import org.technologybrewery.habushu.util.TomlUtils;

/**
 * Migrates the [tool.poetry.dev-dependencies] group to [tool.poetry.group.dev.dependencies] per preferred Poetry 1.2.0+
 * approach as noted on https://python-poetry.org/docs/managing-dependencies/.
 */
public class DevDependenciesGroupMigration extends AbstractTomlGroupMigration {

    @Override
    protected String getLegacyGroupName() {
        return "tool.poetry.dev-dependencies";
    }

    @Override
    protected String getNewGroupName() {
        return TomlUtils.TOOL_POETRY_DEV_DEPENDENCIES;
    }
}
