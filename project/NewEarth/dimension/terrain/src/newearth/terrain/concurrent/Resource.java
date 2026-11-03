/* SPDX-License-Identifier: MIT; adapted from ReTerraForged. See META-INF/LICENSE-ReTerraForged.txt. */
package newearth.terrain.concurrent;

import newearth.terrain.concurrent.cache.SafeCloseable;

public interface Resource<T> extends SafeCloseable {
    public static final Resource<?> NONE = new Resource<>() {
        @Override
        public Object get() {
            return null;
        }
        
        @Override
        public boolean isOpen() {
            return false;
        }
        
        @Override
        public void close() {
        }
    };
    
    T get();
    
    boolean isOpen();
    
    @SuppressWarnings("unchecked")
	public static <T> Resource<T> empty() {
        return (Resource<T>) Resource.NONE;
    }
}
