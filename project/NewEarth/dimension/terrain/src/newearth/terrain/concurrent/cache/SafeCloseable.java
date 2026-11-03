/* SPDX-License-Identifier: MIT; adapted from ReTerraForged. See META-INF/LICENSE-ReTerraForged.txt. */
package newearth.terrain.concurrent.cache;

public interface SafeCloseable extends AutoCloseable {
    void close();
}
