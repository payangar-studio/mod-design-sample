/**
 * The shared library of the mods, copied into each mod under {@code <package>.vendor}.
 *
 * <p>In mc-cli's templates, this package is a template package: the copy rewrites its name to
 * the host mod's {@code <package>.vendor}, in paths and wherever it appears in a file. Several
 * mods carry their own copy and run together, so the code here:
 *
 * <ul>
 *   <li>puts no resource under a fixed shared path without the host mod id namespace;
 *   <li>names network payloads with the host mod id;
 *   <li>writes no package or class name in a string, since the package is rewritten;
 *   <li>has no mixin, unless its mixin configuration is generated per mod.
 * </ul>
 */
package com.payangar.designsample.vendor;
