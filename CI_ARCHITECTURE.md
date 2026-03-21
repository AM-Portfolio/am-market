# 🏗️ am-market CI/CD Architecture: Monorepo with Path Filtering

This document explains the architectural decision behind keeping all `am-market` services (UI, Parser, Common Data Models, etc.) within a single repository (a **Monorepo**) and how we optimize our CI/CD pipelines using **Path Filtering**.

## 🎯 The Chosen Design
We have chosen the **Monorepo with Path Filtering** approach. 
This means all related code for `am-market` lives in one Git repository, but our CI/CD pipelines are intelligent enough to only build and deploy the specific microservices that actually changed.

### Why not use Multi-Repo or Git Submodules?
- **Multi-Repo (Polyrepo)** creates "versioning hell." If you update `am-common-investment-model`, you'd have to publish it, go to the `am-market-data-parser` repo, update the `pom.xml`, and rebuild.
- **Git Submodules** are confusing to maintain, often cause detached HEAD errors, and don't play well with most CI/CD runners natively.

---

## 🚀 The Benefits of This Approach

1. **Atomic Commits (High Developer Velocity)**
   You can change the core Java data model the `UI` and the `Parser` all in one single Pull Request. You don't have to jump across three different repositories to implement one feature crossing the stack.

2. **Easy Refactoring**
   Developers can open `am-market` in their IDE and globally search, replace, and refactor code across all microservices instantly.

3. **Zero Unnecessary Builds**
   Because we use Path Filtering in our CI/CD tool, we completely eliminate the downside of a Monorepo. Modifying a parser file will **never** trigger a UI container build.

---

## ⚙️ How Path Filtering Works
Path filtering is a native feature in modern CI/CD tools (like GitHub Actions, GitLab CI, etc.). When a commit is pushed, the CI engine looks at the files modified in that commit. If the modified files match the paths declared in a workflow, that workflow runs. Otherwise, it is skipped.

### 📝 Example Workflow: Changing a single Data Model
1. A developer modifies `am-market/am-common-investment-data/am-common-investment-model/src/main/java/.../Model.java`.
2. They push the commit.
3. The **UI Workflow** checks the commit: *"Did anything in the UI folder change?"* -> **No.** (UI build is skipped).
4. The **Parser Workflow** checks the commit: *"Did anything in the Parser folder OR the shared Model folder change?"* -> **Yes.** (Parser rebuilds pulling the new model).

---

## 🛠️ Pipeline Strategy: One File vs. Multiple Files?
**Question:** Should we have one giant workflow file that builds everything, or separate pipeline files for each module?

**Answer: Use Separate Pipeline Files for Each Module.**

It is highly recommended to create **one separate CI/CD workflow file per module/service** (e.g., `build-ui.yml`, `build-parser.yml`, `build-redis.yml`). 

### Why Multiple Files are Better:
1. **Cleaner Code:** A single workflow file trying to build 10 different containers with complex `if/else` logic becomes completely unreadable.
2. **Different Build Tools:** The UI might use `npm` and `Docker`, while the Parser uses `mvn cache`, `Java 17`, and `Docker`. Keeping them in separate files ensures they don't share messy environments.
3. **Independent Triggering:**
   Each file independently declares what triggers it. 

**Example `build-ui.yml`:**
```yaml
on:
  push:
    paths:
      - 'am-market-ui/**' # ONLY triggers when UI code is touched
```

**Example `build-parser.yml`:**
```yaml
on:
  push:
    paths:
      - 'am-market-data/market-data-parser/**' # Triggers for parser changes
      - 'am-common-investment-data/**'         # AND triggers if the shared base models change
```

By keeping pipelines separated per container/service, you achieve the perfect balance: the **development ease** of a Monorepo with the **build isolation** of a Multi-repo.

---

## 📦 What about SDKs and Libraries?
You are absolutely right: **SDKs (like `am-market-sdk` for Java, Python, or Flutter) and shared libraries do NOT need Docker containers.**

They are not standalone applications that run on a server. They are code dependencies that other applications import. 

### How CI deals with SDKs/Libraries:
Instead of a `docker build` step, the CI workflow for an SDK will run a **Publish** step. 
For example, if you change a class in `am-market-sdk`:
1. The `build-sdk.yml` pipeline triggers.
2. It compiles the Java code into a `.jar` file.
3. It **publishes** that `.jar` to a package registry (like GitHub Packages or Maven Central). 
4. Other applications (like your UI or Parser) can now download the new SDK version during their own container builds.
