Feature: Provides ability to push .dev versions to a different repository than released versions

  Scenario: Enable and use the default dev repository (test.pypi.org)
    Given use development repositories are enabled
    When the Habushu deploy phase executes
    Then the dev repository url is "https://test.pypi.org/legacy/"

  Scenario Outline: Enable and use a custom dev repository
    Given use development repositories are enabled
    And a custom dev repository is configured to "<customDevRepositoryUrl>"
    When the Habushu deploy phase executes
    Then the dev repository url is "<finalUploadDevRepositoryUrl>"

    Examples:
      | customDevRepositoryUrl                  | finalUploadDevRepositoryUrl                    |
      | https://nexus.github.com/habushu/       | https://nexus.github.com/habushu/legacy/       |
      | https://artifactory.github.com/habushu/ | https://artifactory.github.com/habushu/legacy/ |
      | https://nexus.noTrailingSlash.com       | https://nexus.noTrailingSlash.com/legacy/      |

  Scenario Outline: Enable and use a custom dev repository with a custom suffix for repositories not using "/legacy/"
    Given use development repositories are enabled
    And a custom dev repository is configured to "<customDevRepositoryUrl>"
    And a custom push dev repository url suffix of "<devRepositoryUrlUploadSuffix>"
    When the Habushu deploy phase executes
    Then the dev repository url is "<finalUploadDevRepositoryUrl>"

    Examples:
      | customDevRepositoryUrl               | devRepositoryUrlUploadSuffix | finalUploadDevRepositoryUrl                  |
      | https://nexus.github.com/habushu/    | not-legacy                   | https://nexus.github.com/habushu/not-legacy/ |
      | https://artifactory.github.com/pypi/ | foo/                         | https://artifactory.github.com/pypi/foo/     |
      | https://test.mypypi.org              | bar                          | https://test.mypypi.org/bar/                 |
      | https://update.myrepo.com/           |                              | https://update.myrepo.com/                   |
      | https://update.myrepo.com            |                              | https://update.myrepo.com/                   |

