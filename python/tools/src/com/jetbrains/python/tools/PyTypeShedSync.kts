// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.jetbrains.python.tools

import com.intellij.util.io.delete
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * This script was implemented to sync local copy of `typeshed` with bundled `typeshed`.
 *
 * As a result it leaves top-level modules and packages that are listed in `whiteList`.
 * It allows us to reduce the size of bundled `typeshed` and do not run indexing and other analyzing processes on disabled stubs.
 *
 * @see [com.jetbrains.python.tools.splitBuiltins]
 */

val repo: Path = Paths.get("../../../../../../../../../typeshed").abs().normalize()
val bundled: Path = Paths.get("../../../../../../../../community/python/helpers/typeshed").abs().normalize()

println("Repo: ${repo.abs()}")
println("Bundled: ${bundled.abs()}")

println("Syncing")
sync(repo, bundled)

val whiteList = sequenceOf(
  "__builtin__",
  "__future__",
  "_codecs",
  "_csv",
  "_curses",
  "_importlib_modulespec",
  "_io",
  "_operator",
  "_thread",
  "abc",
  "argparse",
  "asyncio",
  "attr",
  "audioop",
  "bdb",
  "binascii",
  "builtins",
  "cmath",
  "cmd",
  "codecs",
  "collections",
  "concurrent",
  "configparser",
  "contextvars",
  "cPickle",
  "crypt",
  "Crypto",
  "csv",
  "ctypes",
  "datetime",
  "dbm",
  "decimal",
  "difflib",
  "distutils",
  "email",
  "exceptions",
  "functools",
  "genericpath",
  "gflags",
  "hashlib",
  "heapq",
  "http",
  "inspect",
  "io",
  "itertools",
  "json",
  "logging",
  "macpath",
  "math",
  "mock",
  "multiprocessing",
  "ntpath",
  "numbers",
  "opcode",
  "operator",
  "os",
  "os2emxpath",
  "pathlib",
  "pdb",
  "pickle",
  "posix",
  "posixpath",
  "pprint",
  "pyexpat",
  "queue",
  "re",
  "requests",
  "shutil",
  "signal",
  "six",
  "socket",
  "sqlite3",
  "ssl",
  "subprocess",
  "sys",
  "threading",
  "time",
  "token",
  "turtle",
  "types",
  "typing",
  "typing_extensions",
  "unittest",
  "urllib",
  "uuid",
  "webbrowser",
  "werkzeug",
  "xml",
  "zlib"
).mapTo(hashSetOf()) { it.toLowerCase() }

println("Cleaning")
cleanTopLevelPackages(bundled, whiteList)

println("Splitting builtins")
splitBuiltins(bundled)

fun sync(repo: Path, bundled: Path) {
  if (!Files.exists(repo)) throw IllegalArgumentException("Not found: ${repo.abs()}")

  if (Files.exists(bundled)) {
    bundled.delete()
    println("Removed: ${bundled.abs()}")
  }

  val whiteList = setOf("stdlib",
                        "tests",
                        "third_party",
                        ".flake8",
                        ".gitignore",
                        ".travis.yml",
                        "CONTRIBUTING.md",
                        "LICENSE",
                        "pyproject.toml",
                        "README.md",
                        "requirements-tests-py3.txt")

  Files
    .newDirectoryStream(repo)
    .forEach {
      if (it.name() in whiteList) {
        val target = bundled.resolve(it.fileName)

        it.copyRecursively(target)
        println("Copied: ${it.abs()} to ${target.abs()}")
      }
      else {
        println("Skipped: ${it.abs()}")
      }
    }
}

fun cleanTopLevelPackages(typeshed: Path, whiteList: Set<String>) {
  sequenceOf(typeshed)
    .flatMap { sequenceOf(it.resolve("stdlib"), it.resolve("third_party")) }
    .flatMap { Files.newDirectoryStream(it).asSequence() }
    .flatMap { Files.newDirectoryStream(it).asSequence() }
    .filter { it.nameWithoutExtension().toLowerCase() !in whiteList }
    .forEach { it.delete() }
}

fun Path.abs() = toAbsolutePath()
fun Path.copyRecursively(target: Path) = toFile().copyRecursively(target.toFile())
fun Path.name() = toFile().name
fun Path.nameWithoutExtension() = toFile().nameWithoutExtension
