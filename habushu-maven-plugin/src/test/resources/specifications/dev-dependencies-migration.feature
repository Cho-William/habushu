Feature: Test automatic tool.poetry.dev-dependencies to tool.poetry.group.dev.dependencies pyproject.toml group

  Scenario: Remove dev-dependencies dependencies when existing group.dev.dependencies group DOES exists
    Given an existing pyproject.toml file with two dev-dependencies dependencies each in the tool.poetry.dev-dependencies and tool.poetry.group.dev.dependencies groups
    When the Habushu dev dependency migration executes
    Then 0 pyproject.toml dependencies exist in the tool.poetry.dev-dependencies group
    And 4 pyproject.toml dependencies exist in the tool.poetry.group.dev.dependencies group

  Scenario: Remove dev-dependencies dependencies does not change a file with non applicable dependencies
    Given an existing pyproject.toml without any dev-dependencies dependencies
    When the Habushu dev dependency migration executes
    Then no migration was performed