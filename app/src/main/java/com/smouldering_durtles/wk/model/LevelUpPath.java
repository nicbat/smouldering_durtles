package com.smouldering_durtles.wk.model;

import com.smouldering_durtles.wk.db.model.Subject;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/** Unpassed current-level kanji and prerequisites, using permanent passing milestones. */
public final class LevelUpPath {
    private LevelUpPath() { }
    public static Set<Long> ids(Collection<Subject> kanji, int userLevel, int maxLevel) {
        Set<Long> result = new HashSet<>();
        if (userLevel <= 0 || userLevel >= maxLevel) return result;
        int total = 0, passed = 0;
        for (Subject subject : kanji) {
            if (subject.getLevel() != userLevel || !subject.getType().isKanji()) continue;
            total++;
            if (subject.isPassed()) passed++;
            else {
                result.add(subject.getId());
                result.addAll(subject.getComponentSubjectIds());
            }
        }
        if (total == 0 || passed >= (total * 9L + 9) / 10) result.clear();
        return result;
    }
    public static boolean includes(Subject subject, Collection<Long> ids) {
        return !subject.isPassed() && (subject.getType().isKanji() || subject.getType().isRadical())
                && ids.contains(subject.getId());
    }
}
