package org.technologybrewery.habushu.migration;

import org.technologybrewery.habushu.util.TomlUtils;

/**
 * Automatically migrates any monorepo dependencies (e.g., foo = {path = "../foo", develop = true}) in the
 * [tool.poetry.group.monorepo.dependencies] group into the [tool.poetry.dependencies] group instead.
 */
public class RemoveMonorepoGroupMigration extends AbstractTomlGroupMigration {

    @Override
    protected String getLegacyGroupName() {
        return TomlUtils.TOOL_POETRY_GROUP_MONOREPO_DEPENDENCIES;
    }

    @Override
    protected String getNewGroupName() {
        return TomlUtils.TOOL_POETRY_DEPENDENCIES;
    }

}
