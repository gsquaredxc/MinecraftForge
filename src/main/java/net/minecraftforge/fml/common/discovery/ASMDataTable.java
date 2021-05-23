/*
 * Forge Mod Loader
 * Copyright (c) 2012-2013 cpw.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 *     cpw - implementation
 */

package net.minecraftforge.fml.common.discovery;

import com.google.common.base.Predicate;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import net.minecraftforge.fml.common.ModContainer;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collector;

public class ASMDataTable {
    public final static class ASMData implements Cloneable {
        private final ModCandidate candidate;
        private final String annotationName;
        private final String className;
        private final String objectName;
        private Map<String, Object> annotationInfo;

        public ASMData(ModCandidate candidate, String annotationName, String className, String objectName, Map<String, Object> info) {
            this.candidate = candidate;
            if (annotationName != null) {
                this.annotationName = annotationName.intern();
            } else {
                this.annotationName = null;
            }
            if (className != null) {
                this.className = className.intern();
            } else {
                this.className = null;
            }
            if (objectName != null) {
                this.objectName = objectName.intern();
            } else {
                this.objectName = null;
            }
            this.annotationInfo = info;
        }

        public ModCandidate getCandidate() {
            return candidate;
        }

        public String getAnnotationName() {
            return annotationName;
        }

        public String getClassName() {
            return className;
        }

        public String getObjectName() {
            return objectName;
        }

        public Map<String, Object> getAnnotationInfo() {
            return annotationInfo;
        }

        public ASMData copy(Map<String, Object> newAnnotationInfo) {
            try {
                ASMData clone = (ASMData) this.clone();
                clone.annotationInfo = newAnnotationInfo;
                return clone;
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException("Impossible", e);
            }
        }
    }

    private static class ModContainerPredicate implements Predicate<ASMData> {
        private final ModContainer container;

        public ModContainerPredicate(ModContainer container) {
            this.container = container;
        }

        @Override
        public boolean apply(ASMData data) {
            return container.getSource().equals(data.candidate.getModContainer());
        }
    }

    private final SetMultimap<String, ASMData> globalAnnotationData = HashMultimap.create();
    private Map<ModContainer, SetMultimap<String, ASMData>> containerAnnotationData;

    private final List<ModContainer> containers = Lists.newArrayList();
    private final SetMultimap<String, ModCandidate> packageMap = HashMultimap.create();

    public SetMultimap<String, ASMData> getAnnotationsFor(ModContainer container) {
        if (containerAnnotationData == null) {
            containerAnnotationData = containers.parallelStream()
                .map(cont -> Pair.of(cont, ImmutableSetMultimap.copyOf(Multimaps.filterValues(globalAnnotationData, new ModContainerPredicate(cont)))))
                .collect(toImmutableMap(Pair::getKey, Pair::getValue));
        }

        return containerAnnotationData.get(container);
    }

    public static <T, K, V> Collector<T, ?, ImmutableMap<K, V>> toImmutableMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper) {
        return Collector.of(
            ImmutableMap.Builder<K, V>::new,
            (builder, entry) -> builder.put(keyMapper.apply(entry), valueMapper.apply(entry)),
            (leftBuild, rightBuild) -> leftBuild.putAll(rightBuild.build()),
            ImmutableMap.Builder::build);
    }

    public Set<ASMData> getAll(String annotation) {
        return globalAnnotationData.get(annotation);
    }

    public void addASMData(ModCandidate candidate, String annotation, String className, String objectName, Map<String, Object> annotationInfo) {
        globalAnnotationData.put(annotation, new ASMData(candidate, annotation, className, objectName, annotationInfo));
    }

    public void addContainer(ModContainer container) {
        this.containers.add(container);
    }

    public void registerPackage(ModCandidate modCandidate, String pkg) {
        this.packageMap.put(pkg, modCandidate);
    }

    public Set<ModCandidate> getCandidatesFor(String pkg) {
        return this.packageMap.get(pkg);
    }
}
