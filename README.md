# Fast Peek

![Build](https://github.com/vsuhanov/fast-peek/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)

## Template ToDo list

- [x] Create a new [IntelliJ Platform Plugin Template][template] project.
- [x] Get familiar with the [template documentation][template].
- [ ] Adjust the [pluginGroup](./gradle.properties) and [pluginName](./gradle.properties), as well as
  the [id](./src/main/resources/META-INF/plugin.xml) and [sources package](./src/main/kotlin).
- [ ] Adjust the plugin description in `README` (see [Tips][docs:plugin-description])
- [ ] Review
  the [Legal Agreements](https://plugins.jetbrains.com/docs/marketplace/legal-agreements.html?from=IJPluginTemplate).
- [ ] [Publish a plugin manually](https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html?from=IJPluginTemplate)
  for the first time.
- [ ] Set the `MARKETPLACE_ID` in the above README badges. You can obtain it once the plugin is published to JetBrains
  Marketplace.
- [ ] Set the [Plugin Signing](https://plugins.jetbrains.com/docs/intellij/plugin-signing.html?from=IJPluginTemplate)
  related [secrets](https://github.com/JetBrains/intellij-platform-plugin-template#environment-variables).
- [ ] Set
  the [Deployment Token](https://plugins.jetbrains.com/docs/marketplace/plugin-upload.html?from=IJPluginTemplate).
- [ ] Click the <kbd>Watch</kbd> button on the top of the [IntelliJ Platform Plugin Template][template] to be notified
  about releases containing new features and fixes.

# Description
<!-- Plugin description -->
Quickly peek a definition of a symbol under the cursor in an editable popup.

It is similar to the `Quick Definition` feature, but is different in following ways:

1. Instead of a short snippet of the definition, it shows the containing file
2. The file is fully editable in the popup so you can quickly adjust
3. Further invocation of the action will continue to go deeper in the same popup


## How to use it

The action is called `Fast Peek`, search it via `Find Action` (ctrl/cmd+shift+a).
You can assign a shortcut to it via the keymap. For vim users action id is: `com.github.vsuhanov.fastpeek.peek`

I use it with ideavimrc via the following mapping:

```
nmap K :action com.github.vsuhanov.fastpeek.peek<CR>
```

I also bind the action to the double-click in the keymap of IntellJ. This allows very convenient navigation with the
mouse.
When I want to see where a variable or a function comes from I can just double-click on it.

Important to note that double-clicking works poorly if you rely on it to do a word selection because the popup will
steal the focus and ctrl/cmd+c will copy the first line from the editor that opened in the popup

# Known limitations
1. currently double-click in the popup that opens is hardcoded to trigger the same action, so you won't be able to do selection in this way in the popup.
2. `GotoDefinition` action (ctrl/cmd+b, ctrl/cmd+click, scroll wheel button click) currently only works via keyboard shortcuts and scroll wheel button click. ctrl/cmd+click does not work.
   * it's also going to open the defition in the main editor, not in the popup. Further navigation with the popup is available via double-click

# What's new
## 1.0.5
1. show a title with the file name on top of the popup
2. in some cases navigation in the editor (if it is in the same file) will actually happen in the popup. 
  * this is work in progress, unfortunately the platform does not support any simple implementation to navigate within the same editor
<!-- Plugin description end -->

## Installation

- Using the IDE built-in plugin system:

  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "fast-peek"</kbd> >
  <kbd>Install</kbd>

- Using JetBrains Marketplace:

  Go to [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID) and install it by clicking
  the <kbd>Install to ...</kbd> button in case your IDE is running.

  You can also download the [latest release](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID/versions) from
  JetBrains Marketplace and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

- Manually:

  Download the [latest release](https://github.com/vsuhanov/fast-peek/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template

[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation
