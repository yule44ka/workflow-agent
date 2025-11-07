YouTrack Workflows
Workflows in YouTrack let you customize and automate the lifecycle of issues in your project. With workflows, you can notify teams about events, enforce policies, execute periodic tasks, and support existing business processes.

Workflows and Apps
Workflows are part of the ecosystem for apps in YouTrack. Like other apps, workflows are used to enhance and customize functionality in YouTrack. When you create, upload, or install a workflow, it is included in the list of apps that are available for use in the system.

Before apps were introduced, workflows were the primary means of customization and automation in YouTrack. To minimize disruption for existing users, we still retain the interfaces for managing workflows separately from apps at the global and project level. However, the options for attaching and activating workflows in specific projects work exactly the same way on both the workflow and app administration pages.

Workflows can be included in an app package alongside widgets for other extension points. When this happens, these workflows are identified by the app that provides their functionality in the workflow list.

Workflows in JavaScript
YouTrack lets you write workflows in JavaScript. You can write a workflow in any IDE that supports JavaScript, pack it into a ZIP file, and upload it to YouTrack.

In addition, we built a web-based workflow editor inside YouTrack. Here, you can write a workflow from scratch without leaving YouTrack.

What's a Workflow?
In YouTrack, a workflow is a set of rules that can be attached to a project. These rules define a lifecycle for issues in a project and automate changes that can be applied to issues.

When you create a workflow, you can attach it to a project and activate specific rules. A workflow can contain several rules, but you can choose which combination of rules you want to activate in different projects. YouTrack lets you attach a workflow to several projects and enable or disable rules for each project individually.

Default Workflows
YouTrack provides several default workflows that cover the most general use cases. For example, workflows that automatically assign an issue to a subsystem owner or process duplicate issue.

Many default workflows are auto-attached. These workflows are attached automatically to all new projects.

Custom Workflows
If you need a workflow that supports a specific use case, you can write your own. You can either customize a default workflow to support your use case or create a new workflow.

You can also use custom workflows that have been uploaded to the Custom Workflow Repository in GitHub.
