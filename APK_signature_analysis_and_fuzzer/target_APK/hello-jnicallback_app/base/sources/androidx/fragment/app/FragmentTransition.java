package androidx.fragment.app;

import android.graphics.Rect;
import android.os.Build;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import androidx.collection.ArrayMap;
import androidx.core.app.SharedElementCallback;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.BackStackRecord;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
/* loaded from: classes.dex */
public class FragmentTransition {
    private static final int[] INVERSE_OPS = {0, 3, 0, 1, 5, 4, 7, 6, 9, 8};
    private static final FragmentTransitionImpl PLATFORM_IMPL;
    private static final FragmentTransitionImpl SUPPORT_IMPL;

    static {
        PLATFORM_IMPL = Build.VERSION.SDK_INT >= 21 ? new FragmentTransitionCompat21() : null;
        SUPPORT_IMPL = resolveSupportImpl();
    }

    private static FragmentTransitionImpl resolveSupportImpl() {
        try {
            return (FragmentTransitionImpl) Class.forName("androidx.transition.FragmentTransitionSupport").getDeclaredConstructor(new Class[0]).newInstance(new Object[0]);
        } catch (Exception e) {
            return null;
        }
    }

    public static void startTransitions(FragmentManagerImpl fragmentManager, ArrayList<BackStackRecord> records, ArrayList<Boolean> isRecordPop, int startIndex, int endIndex, boolean isReordered) {
        if (fragmentManager.mCurState < 1) {
            return;
        }
        SparseArray<FragmentContainerTransition> transitioningFragments = new SparseArray<>();
        for (int i = startIndex; i < endIndex; i++) {
            BackStackRecord record = records.get(i);
            boolean isPop = isRecordPop.get(i).booleanValue();
            if (isPop) {
                calculatePopFragments(record, transitioningFragments, isReordered);
            } else {
                calculateFragments(record, transitioningFragments, isReordered);
            }
        }
        int i2 = transitioningFragments.size();
        if (i2 != 0) {
            View nonExistentView = new View(fragmentManager.mHost.getContext());
            int numContainers = transitioningFragments.size();
            for (int i3 = 0; i3 < numContainers; i3++) {
                int containerId = transitioningFragments.keyAt(i3);
                ArrayMap<String, String> nameOverrides = calculateNameOverrides(containerId, records, isRecordPop, startIndex, endIndex);
                FragmentContainerTransition containerTransition = transitioningFragments.valueAt(i3);
                if (isReordered) {
                    configureTransitionsReordered(fragmentManager, containerId, containerTransition, nonExistentView, nameOverrides);
                } else {
                    configureTransitionsOrdered(fragmentManager, containerId, containerTransition, nonExistentView, nameOverrides);
                }
            }
        }
    }

    private static ArrayMap<String, String> calculateNameOverrides(int containerId, ArrayList<BackStackRecord> records, ArrayList<Boolean> isRecordPop, int startIndex, int endIndex) {
        ArrayList<String> sources;
        ArrayList<String> targets;
        ArrayMap<String, String> nameOverrides = new ArrayMap<>();
        for (int recordNum = endIndex - 1; recordNum >= startIndex; recordNum--) {
            BackStackRecord record = records.get(recordNum);
            if (record.interactsWith(containerId)) {
                boolean isPop = isRecordPop.get(recordNum).booleanValue();
                if (record.mSharedElementSourceNames != null) {
                    int numSharedElements = record.mSharedElementSourceNames.size();
                    if (isPop) {
                        targets = record.mSharedElementSourceNames;
                        sources = record.mSharedElementTargetNames;
                    } else {
                        sources = record.mSharedElementSourceNames;
                        targets = record.mSharedElementTargetNames;
                    }
                    for (int i = 0; i < numSharedElements; i++) {
                        String sourceName = sources.get(i);
                        String targetName = targets.get(i);
                        String previousTarget = nameOverrides.remove(targetName);
                        if (previousTarget != null) {
                            nameOverrides.put(sourceName, previousTarget);
                        } else {
                            nameOverrides.put(sourceName, targetName);
                        }
                    }
                }
            }
        }
        return nameOverrides;
    }

    private static void configureTransitionsReordered(FragmentManagerImpl fragmentManager, int containerId, FragmentContainerTransition fragments, View nonExistentView, ArrayMap<String, String> nameOverrides) {
        Fragment inFragment;
        Fragment outFragment;
        FragmentTransitionImpl impl;
        Object exitTransition;
        ViewGroup sceneRoot = fragmentManager.mContainer.onHasView() ? (ViewGroup) fragmentManager.mContainer.onFindViewById(containerId) : null;
        if (sceneRoot == null || (impl = chooseImpl((outFragment = fragments.firstOut), (inFragment = fragments.lastIn))) == null) {
            return;
        }
        boolean inIsPop = fragments.lastInIsPop;
        boolean outIsPop = fragments.firstOutIsPop;
        ArrayList<View> sharedElementsIn = new ArrayList<>();
        ArrayList<View> sharedElementsOut = new ArrayList<>();
        Object enterTransition = getEnterTransition(impl, inFragment, inIsPop);
        Object exitTransition2 = getExitTransition(impl, outFragment, outIsPop);
        Object sharedElementTransition = configureSharedElementsReordered(impl, sceneRoot, nonExistentView, nameOverrides, fragments, sharedElementsOut, sharedElementsIn, enterTransition, exitTransition2);
        if (enterTransition == null && sharedElementTransition == null) {
            exitTransition = exitTransition2;
            if (exitTransition == null) {
                return;
            }
        } else {
            exitTransition = exitTransition2;
        }
        ArrayList<View> exitingViews = configureEnteringExitingViews(impl, exitTransition, outFragment, sharedElementsOut, nonExistentView);
        ArrayList<View> enteringViews = configureEnteringExitingViews(impl, enterTransition, inFragment, sharedElementsIn, nonExistentView);
        setViewVisibility(enteringViews, 4);
        Object transition = mergeTransitions(impl, enterTransition, exitTransition, sharedElementTransition, inFragment, inIsPop);
        if (transition != null) {
            replaceHide(impl, exitTransition, outFragment, exitingViews);
            ArrayList<String> inNames = impl.prepareSetNameOverridesReordered(sharedElementsIn);
            impl.scheduleRemoveTargets(transition, enterTransition, enteringViews, exitTransition, exitingViews, sharedElementTransition, sharedElementsIn);
            impl.beginDelayedTransition(sceneRoot, transition);
            impl.setNameOverridesReordered(sceneRoot, sharedElementsOut, sharedElementsIn, inNames, nameOverrides);
            setViewVisibility(enteringViews, 0);
            impl.swapSharedElementTargets(sharedElementTransition, sharedElementsOut, sharedElementsIn);
        }
    }

    private static void replaceHide(FragmentTransitionImpl impl, Object exitTransition, Fragment exitingFragment, final ArrayList<View> exitingViews) {
        if (exitingFragment != null && exitTransition != null && exitingFragment.mAdded && exitingFragment.mHidden && exitingFragment.mHiddenChanged) {
            exitingFragment.setHideReplaced(true);
            impl.scheduleHideFragmentView(exitTransition, exitingFragment.getView(), exitingViews);
            ViewGroup container = exitingFragment.mContainer;
            OneShotPreDrawListener.add(container, new Runnable() { // from class: androidx.fragment.app.FragmentTransition.1
                @Override // java.lang.Runnable
                public void run() {
                    FragmentTransition.setViewVisibility(exitingViews, 4);
                }
            });
        }
    }

    private static void configureTransitionsOrdered(FragmentManagerImpl fragmentManager, int containerId, FragmentContainerTransition fragments, View nonExistentView, ArrayMap<String, String> nameOverrides) {
        Fragment inFragment;
        Fragment outFragment;
        FragmentTransitionImpl impl;
        Object exitTransition;
        ViewGroup sceneRoot = fragmentManager.mContainer.onHasView() ? (ViewGroup) fragmentManager.mContainer.onFindViewById(containerId) : null;
        if (sceneRoot == null || (impl = chooseImpl((outFragment = fragments.firstOut), (inFragment = fragments.lastIn))) == null) {
            return;
        }
        boolean inIsPop = fragments.lastInIsPop;
        boolean outIsPop = fragments.firstOutIsPop;
        Object enterTransition = getEnterTransition(impl, inFragment, inIsPop);
        Object exitTransition2 = getExitTransition(impl, outFragment, outIsPop);
        ArrayList<View> sharedElementsOut = new ArrayList<>();
        ArrayList<View> sharedElementsIn = new ArrayList<>();
        Object sharedElementTransition = configureSharedElementsOrdered(impl, sceneRoot, nonExistentView, nameOverrides, fragments, sharedElementsOut, sharedElementsIn, enterTransition, exitTransition2);
        if (enterTransition == null && sharedElementTransition == null) {
            exitTransition = exitTransition2;
            if (exitTransition == null) {
                return;
            }
        } else {
            exitTransition = exitTransition2;
        }
        ArrayList<View> exitingViews = configureEnteringExitingViews(impl, exitTransition, outFragment, sharedElementsOut, nonExistentView);
        Object exitTransition3 = (exitingViews == null || exitingViews.isEmpty()) ? null : exitTransition;
        impl.addTarget(enterTransition, nonExistentView);
        Object transition = mergeTransitions(impl, enterTransition, exitTransition3, sharedElementTransition, inFragment, fragments.lastInIsPop);
        if (transition != null) {
            ArrayList<View> enteringViews = new ArrayList<>();
            impl.scheduleRemoveTargets(transition, enterTransition, enteringViews, exitTransition3, exitingViews, sharedElementTransition, sharedElementsIn);
            scheduleTargetChange(impl, sceneRoot, inFragment, nonExistentView, sharedElementsIn, enterTransition, enteringViews, exitTransition3, exitingViews);
            impl.setNameOverridesOrdered(sceneRoot, sharedElementsIn, nameOverrides);
            impl.beginDelayedTransition(sceneRoot, transition);
            impl.scheduleNameReset(sceneRoot, sharedElementsIn, nameOverrides);
        }
    }

    private static void scheduleTargetChange(final FragmentTransitionImpl impl, ViewGroup sceneRoot, final Fragment inFragment, final View nonExistentView, final ArrayList<View> sharedElementsIn, final Object enterTransition, final ArrayList<View> enteringViews, final Object exitTransition, final ArrayList<View> exitingViews) {
        OneShotPreDrawListener.add(sceneRoot, new Runnable() { // from class: androidx.fragment.app.FragmentTransition.2
            @Override // java.lang.Runnable
            public void run() {
                Object obj = enterTransition;
                if (obj != null) {
                    impl.removeTarget(obj, nonExistentView);
                    ArrayList<View> views = FragmentTransition.configureEnteringExitingViews(impl, enterTransition, inFragment, sharedElementsIn, nonExistentView);
                    enteringViews.addAll(views);
                }
                ArrayList<View> views2 = exitingViews;
                if (views2 != null) {
                    if (exitTransition != null) {
                        ArrayList<View> tempExiting = new ArrayList<>();
                        tempExiting.add(nonExistentView);
                        impl.replaceTargets(exitTransition, exitingViews, tempExiting);
                    }
                    exitingViews.clear();
                    exitingViews.add(nonExistentView);
                }
            }
        });
    }

    private static FragmentTransitionImpl chooseImpl(Fragment outFragment, Fragment inFragment) {
        ArrayList<Object> transitions = new ArrayList<>();
        if (outFragment != null) {
            Object exitTransition = outFragment.getExitTransition();
            if (exitTransition != null) {
                transitions.add(exitTransition);
            }
            Object returnTransition = outFragment.getReturnTransition();
            if (returnTransition != null) {
                transitions.add(returnTransition);
            }
            Object sharedReturnTransition = outFragment.getSharedElementReturnTransition();
            if (sharedReturnTransition != null) {
                transitions.add(sharedReturnTransition);
            }
        }
        if (inFragment != null) {
            Object enterTransition = inFragment.getEnterTransition();
            if (enterTransition != null) {
                transitions.add(enterTransition);
            }
            Object reenterTransition = inFragment.getReenterTransition();
            if (reenterTransition != null) {
                transitions.add(reenterTransition);
            }
            Object sharedEnterTransition = inFragment.getSharedElementEnterTransition();
            if (sharedEnterTransition != null) {
                transitions.add(sharedEnterTransition);
            }
        }
        if (transitions.isEmpty()) {
            return null;
        }
        FragmentTransitionImpl fragmentTransitionImpl = PLATFORM_IMPL;
        if (fragmentTransitionImpl != null && canHandleAll(fragmentTransitionImpl, transitions)) {
            return fragmentTransitionImpl;
        }
        FragmentTransitionImpl fragmentTransitionImpl2 = SUPPORT_IMPL;
        if (fragmentTransitionImpl2 != null && canHandleAll(fragmentTransitionImpl2, transitions)) {
            return fragmentTransitionImpl2;
        }
        if (fragmentTransitionImpl != null || fragmentTransitionImpl2 != null) {
            throw new IllegalArgumentException("Invalid Transition types");
        }
        return null;
    }

    private static boolean canHandleAll(FragmentTransitionImpl impl, List<Object> transitions) {
        int size = transitions.size();
        for (int i = 0; i < size; i++) {
            if (!impl.canHandle(transitions.get(i))) {
                return false;
            }
        }
        return true;
    }

    private static Object getSharedElementTransition(FragmentTransitionImpl impl, Fragment inFragment, Fragment outFragment, boolean isPop) {
        Object obj;
        if (inFragment == null || outFragment == null) {
            return null;
        }
        if (isPop) {
            obj = outFragment.getSharedElementReturnTransition();
        } else {
            obj = inFragment.getSharedElementEnterTransition();
        }
        Object transition = impl.cloneTransition(obj);
        return impl.wrapTransitionInSet(transition);
    }

    private static Object getEnterTransition(FragmentTransitionImpl impl, Fragment inFragment, boolean isPop) {
        Object obj;
        if (inFragment == null) {
            return null;
        }
        if (isPop) {
            obj = inFragment.getReenterTransition();
        } else {
            obj = inFragment.getEnterTransition();
        }
        return impl.cloneTransition(obj);
    }

    private static Object getExitTransition(FragmentTransitionImpl impl, Fragment outFragment, boolean isPop) {
        Object obj;
        if (outFragment == null) {
            return null;
        }
        if (isPop) {
            obj = outFragment.getReturnTransition();
        } else {
            obj = outFragment.getExitTransition();
        }
        return impl.cloneTransition(obj);
    }

    private static Object configureSharedElementsReordered(final FragmentTransitionImpl impl, ViewGroup sceneRoot, View nonExistentView, ArrayMap<String, String> nameOverrides, FragmentContainerTransition fragments, ArrayList<View> sharedElementsOut, ArrayList<View> sharedElementsIn, Object enterTransition, Object exitTransition) {
        Object sharedElementTransition;
        Object sharedElementTransition2;
        Object sharedElementTransition3;
        View epicenterView;
        Rect epicenter;
        ArrayMap<String, View> inSharedElements;
        final Fragment inFragment = fragments.lastIn;
        final Fragment outFragment = fragments.firstOut;
        if (inFragment != null) {
            inFragment.getView().setVisibility(0);
        }
        if (inFragment != null && outFragment != null) {
            final boolean inIsPop = fragments.lastInIsPop;
            if (nameOverrides.isEmpty()) {
                sharedElementTransition = null;
            } else {
                sharedElementTransition = getSharedElementTransition(impl, inFragment, outFragment, inIsPop);
            }
            ArrayMap<String, View> outSharedElements = captureOutSharedElements(impl, nameOverrides, sharedElementTransition, fragments);
            ArrayMap<String, View> inSharedElements2 = captureInSharedElements(impl, nameOverrides, sharedElementTransition, fragments);
            if (nameOverrides.isEmpty()) {
                if (outSharedElements != null) {
                    outSharedElements.clear();
                }
                if (inSharedElements2 != null) {
                    inSharedElements2.clear();
                }
                sharedElementTransition2 = null;
            } else {
                addSharedElementsWithMatchingNames(sharedElementsOut, outSharedElements, nameOverrides.keySet());
                addSharedElementsWithMatchingNames(sharedElementsIn, inSharedElements2, nameOverrides.values());
                sharedElementTransition2 = sharedElementTransition;
            }
            if (enterTransition == null && exitTransition == null && sharedElementTransition2 == null) {
                return null;
            }
            callSharedElementStartEnd(inFragment, outFragment, inIsPop, outSharedElements, true);
            if (sharedElementTransition2 != null) {
                sharedElementsIn.add(nonExistentView);
                impl.setSharedElementTargets(sharedElementTransition2, nonExistentView, sharedElementsOut);
                boolean outIsPop = fragments.firstOutIsPop;
                BackStackRecord outTransaction = fragments.firstOutTransaction;
                sharedElementTransition3 = sharedElementTransition2;
                inSharedElements = inSharedElements2;
                setOutEpicenter(impl, sharedElementTransition2, exitTransition, outSharedElements, outIsPop, outTransaction);
                Rect epicenter2 = new Rect();
                View epicenterView2 = getInEpicenterView(inSharedElements, fragments, enterTransition, inIsPop);
                if (epicenterView2 != null) {
                    impl.setEpicenter(enterTransition, epicenter2);
                }
                epicenter = epicenter2;
                epicenterView = epicenterView2;
            } else {
                sharedElementTransition3 = sharedElementTransition2;
                inSharedElements = inSharedElements2;
                epicenter = null;
                epicenterView = null;
            }
            final ArrayMap<String, View> arrayMap = inSharedElements;
            final View view = epicenterView;
            final Rect rect = epicenter;
            OneShotPreDrawListener.add(sceneRoot, new Runnable() { // from class: androidx.fragment.app.FragmentTransition.3
                @Override // java.lang.Runnable
                public void run() {
                    FragmentTransition.callSharedElementStartEnd(inFragment, outFragment, inIsPop, arrayMap, false);
                    View view2 = view;
                    if (view2 != null) {
                        impl.getBoundsOnScreen(view2, rect);
                    }
                }
            });
            return sharedElementTransition3;
        }
        return null;
    }

    private static void addSharedElementsWithMatchingNames(ArrayList<View> views, ArrayMap<String, View> sharedElements, Collection<String> nameOverridesSet) {
        for (int i = sharedElements.size() - 1; i >= 0; i--) {
            View view = sharedElements.valueAt(i);
            if (nameOverridesSet.contains(ViewCompat.getTransitionName(view))) {
                views.add(view);
            }
        }
    }

    private static Object configureSharedElementsOrdered(final FragmentTransitionImpl impl, ViewGroup sceneRoot, final View nonExistentView, final ArrayMap<String, String> nameOverrides, final FragmentContainerTransition fragments, final ArrayList<View> sharedElementsOut, final ArrayList<View> sharedElementsIn, final Object enterTransition, Object exitTransition) {
        Object sharedElementTransition;
        Object sharedElementTransition2;
        Rect inEpicenter;
        final Fragment inFragment = fragments.lastIn;
        final Fragment outFragment = fragments.firstOut;
        if (inFragment != null && outFragment != null) {
            final boolean inIsPop = fragments.lastInIsPop;
            if (nameOverrides.isEmpty()) {
                sharedElementTransition = null;
            } else {
                sharedElementTransition = getSharedElementTransition(impl, inFragment, outFragment, inIsPop);
            }
            ArrayMap<String, View> outSharedElements = captureOutSharedElements(impl, nameOverrides, sharedElementTransition, fragments);
            if (nameOverrides.isEmpty()) {
                sharedElementTransition2 = null;
            } else {
                sharedElementsOut.addAll(outSharedElements.values());
                sharedElementTransition2 = sharedElementTransition;
            }
            if (enterTransition == null && exitTransition == null && sharedElementTransition2 == null) {
                return null;
            }
            callSharedElementStartEnd(inFragment, outFragment, inIsPop, outSharedElements, true);
            if (sharedElementTransition2 != null) {
                Rect inEpicenter2 = new Rect();
                impl.setSharedElementTargets(sharedElementTransition2, nonExistentView, sharedElementsOut);
                boolean outIsPop = fragments.firstOutIsPop;
                BackStackRecord outTransaction = fragments.firstOutTransaction;
                setOutEpicenter(impl, sharedElementTransition2, exitTransition, outSharedElements, outIsPop, outTransaction);
                if (enterTransition != null) {
                    impl.setEpicenter(enterTransition, inEpicenter2);
                }
                inEpicenter = inEpicenter2;
            } else {
                inEpicenter = null;
            }
            final Object finalSharedElementTransition = sharedElementTransition2;
            Object sharedElementTransition3 = sharedElementTransition2;
            final Rect rect = inEpicenter;
            OneShotPreDrawListener.add(sceneRoot, new Runnable() { // from class: androidx.fragment.app.FragmentTransition.4
                @Override // java.lang.Runnable
                public void run() {
                    ArrayMap<String, View> inSharedElements = FragmentTransition.captureInSharedElements(impl, nameOverrides, finalSharedElementTransition, fragments);
                    if (inSharedElements != null) {
                        sharedElementsIn.addAll(inSharedElements.values());
                        sharedElementsIn.add(nonExistentView);
                    }
                    FragmentTransition.callSharedElementStartEnd(inFragment, outFragment, inIsPop, inSharedElements, false);
                    Object obj = finalSharedElementTransition;
                    if (obj != null) {
                        impl.swapSharedElementTargets(obj, sharedElementsOut, sharedElementsIn);
                        View inEpicenterView = FragmentTransition.getInEpicenterView(inSharedElements, fragments, enterTransition, inIsPop);
                        if (inEpicenterView != null) {
                            impl.getBoundsOnScreen(inEpicenterView, rect);
                        }
                    }
                }
            });
            return sharedElementTransition3;
        }
        return null;
    }

    private static ArrayMap<String, View> captureOutSharedElements(FragmentTransitionImpl impl, ArrayMap<String, String> nameOverrides, Object sharedElementTransition, FragmentContainerTransition fragments) {
        ArrayList<String> names;
        SharedElementCallback sharedElementCallback;
        if (nameOverrides.isEmpty() || sharedElementTransition == null) {
            nameOverrides.clear();
            return null;
        }
        Fragment outFragment = fragments.firstOut;
        ArrayMap<String, View> outSharedElements = new ArrayMap<>();
        impl.findNamedViews(outSharedElements, outFragment.getView());
        BackStackRecord outTransaction = fragments.firstOutTransaction;
        if (fragments.firstOutIsPop) {
            sharedElementCallback = outFragment.getEnterTransitionCallback();
            names = outTransaction.mSharedElementTargetNames;
        } else {
            sharedElementCallback = outFragment.getExitTransitionCallback();
            names = outTransaction.mSharedElementSourceNames;
        }
        outSharedElements.retainAll(names);
        if (sharedElementCallback != null) {
            sharedElementCallback.onMapSharedElements(names, outSharedElements);
            for (int i = names.size() - 1; i >= 0; i--) {
                String name = names.get(i);
                View view = outSharedElements.get(name);
                if (view == null) {
                    nameOverrides.remove(name);
                } else if (!name.equals(ViewCompat.getTransitionName(view))) {
                    String targetValue = nameOverrides.remove(name);
                    nameOverrides.put(ViewCompat.getTransitionName(view), targetValue);
                }
            }
        } else {
            nameOverrides.retainAll(outSharedElements.keySet());
        }
        return outSharedElements;
    }

    static ArrayMap<String, View> captureInSharedElements(FragmentTransitionImpl impl, ArrayMap<String, String> nameOverrides, Object sharedElementTransition, FragmentContainerTransition fragments) {
        ArrayList<String> names;
        SharedElementCallback sharedElementCallback;
        String key;
        Fragment inFragment = fragments.lastIn;
        View fragmentView = inFragment.getView();
        if (nameOverrides.isEmpty() || sharedElementTransition == null || fragmentView == null) {
            nameOverrides.clear();
            return null;
        }
        ArrayMap<String, View> inSharedElements = new ArrayMap<>();
        impl.findNamedViews(inSharedElements, fragmentView);
        BackStackRecord inTransaction = fragments.lastInTransaction;
        if (fragments.lastInIsPop) {
            sharedElementCallback = inFragment.getExitTransitionCallback();
            names = inTransaction.mSharedElementSourceNames;
        } else {
            sharedElementCallback = inFragment.getEnterTransitionCallback();
            names = inTransaction.mSharedElementTargetNames;
        }
        if (names != null) {
            inSharedElements.retainAll(names);
            inSharedElements.retainAll(nameOverrides.values());
        }
        if (sharedElementCallback != null) {
            sharedElementCallback.onMapSharedElements(names, inSharedElements);
            for (int i = names.size() - 1; i >= 0; i--) {
                String name = names.get(i);
                View view = inSharedElements.get(name);
                if (view == null) {
                    String key2 = findKeyForValue(nameOverrides, name);
                    if (key2 != null) {
                        nameOverrides.remove(key2);
                    }
                } else if (!name.equals(ViewCompat.getTransitionName(view)) && (key = findKeyForValue(nameOverrides, name)) != null) {
                    nameOverrides.put(key, ViewCompat.getTransitionName(view));
                }
            }
        } else {
            retainValues(nameOverrides, inSharedElements);
        }
        return inSharedElements;
    }

    private static String findKeyForValue(ArrayMap<String, String> map, String value) {
        int numElements = map.size();
        for (int i = 0; i < numElements; i++) {
            if (value.equals(map.valueAt(i))) {
                return map.keyAt(i);
            }
        }
        return null;
    }

    static View getInEpicenterView(ArrayMap<String, View> inSharedElements, FragmentContainerTransition fragments, Object enterTransition, boolean inIsPop) {
        String targetName;
        BackStackRecord inTransaction = fragments.lastInTransaction;
        if (enterTransition != null && inSharedElements != null && inTransaction.mSharedElementSourceNames != null && !inTransaction.mSharedElementSourceNames.isEmpty()) {
            if (inIsPop) {
                targetName = inTransaction.mSharedElementSourceNames.get(0);
            } else {
                targetName = inTransaction.mSharedElementTargetNames.get(0);
            }
            return inSharedElements.get(targetName);
        }
        return null;
    }

    private static void setOutEpicenter(FragmentTransitionImpl impl, Object sharedElementTransition, Object exitTransition, ArrayMap<String, View> outSharedElements, boolean outIsPop, BackStackRecord outTransaction) {
        String sourceName;
        if (outTransaction.mSharedElementSourceNames != null && !outTransaction.mSharedElementSourceNames.isEmpty()) {
            if (outIsPop) {
                sourceName = outTransaction.mSharedElementTargetNames.get(0);
            } else {
                sourceName = outTransaction.mSharedElementSourceNames.get(0);
            }
            View outEpicenterView = outSharedElements.get(sourceName);
            impl.setEpicenter(sharedElementTransition, outEpicenterView);
            if (exitTransition != null) {
                impl.setEpicenter(exitTransition, outEpicenterView);
            }
        }
    }

    private static void retainValues(ArrayMap<String, String> nameOverrides, ArrayMap<String, View> namedViews) {
        for (int i = nameOverrides.size() - 1; i >= 0; i--) {
            String targetName = nameOverrides.valueAt(i);
            if (!namedViews.containsKey(targetName)) {
                nameOverrides.removeAt(i);
            }
        }
    }

    static void callSharedElementStartEnd(Fragment inFragment, Fragment outFragment, boolean isPop, ArrayMap<String, View> sharedElements, boolean isStart) {
        SharedElementCallback sharedElementCallback;
        if (isPop) {
            sharedElementCallback = outFragment.getEnterTransitionCallback();
        } else {
            sharedElementCallback = inFragment.getEnterTransitionCallback();
        }
        if (sharedElementCallback != null) {
            ArrayList<View> views = new ArrayList<>();
            ArrayList<String> names = new ArrayList<>();
            int count = sharedElements == null ? 0 : sharedElements.size();
            for (int i = 0; i < count; i++) {
                names.add(sharedElements.keyAt(i));
                views.add(sharedElements.valueAt(i));
            }
            if (isStart) {
                sharedElementCallback.onSharedElementStart(names, views, null);
            } else {
                sharedElementCallback.onSharedElementEnd(names, views, null);
            }
        }
    }

    static ArrayList<View> configureEnteringExitingViews(FragmentTransitionImpl impl, Object transition, Fragment fragment, ArrayList<View> sharedElements, View nonExistentView) {
        ArrayList<View> viewList = null;
        if (transition != null) {
            viewList = new ArrayList<>();
            View root = fragment.getView();
            if (root != null) {
                impl.captureTransitioningViews(viewList, root);
            }
            if (sharedElements != null) {
                viewList.removeAll(sharedElements);
            }
            if (!viewList.isEmpty()) {
                viewList.add(nonExistentView);
                impl.addTargets(transition, viewList);
            }
        }
        return viewList;
    }

    static void setViewVisibility(ArrayList<View> views, int visibility) {
        if (views == null) {
            return;
        }
        for (int i = views.size() - 1; i >= 0; i--) {
            View view = views.get(i);
            view.setVisibility(visibility);
        }
    }

    private static Object mergeTransitions(FragmentTransitionImpl impl, Object enterTransition, Object exitTransition, Object sharedElementTransition, Fragment inFragment, boolean isPop) {
        boolean overlap = true;
        if (enterTransition != null && exitTransition != null && inFragment != null) {
            overlap = isPop ? inFragment.getAllowReturnTransitionOverlap() : inFragment.getAllowEnterTransitionOverlap();
        }
        if (overlap) {
            Object transition = impl.mergeTransitionsTogether(exitTransition, enterTransition, sharedElementTransition);
            return transition;
        }
        Object transition2 = impl.mergeTransitionsInSequence(exitTransition, enterTransition, sharedElementTransition);
        return transition2;
    }

    public static void calculateFragments(BackStackRecord transaction, SparseArray<FragmentContainerTransition> transitioningFragments, boolean isReordered) {
        int numOps = transaction.mOps.size();
        for (int opNum = 0; opNum < numOps; opNum++) {
            BackStackRecord.Op op = transaction.mOps.get(opNum);
            addToFirstInLastOut(transaction, op, transitioningFragments, false, isReordered);
        }
    }

    public static void calculatePopFragments(BackStackRecord transaction, SparseArray<FragmentContainerTransition> transitioningFragments, boolean isReordered) {
        if (!transaction.mManager.mContainer.onHasView()) {
            return;
        }
        int numOps = transaction.mOps.size();
        for (int opNum = numOps - 1; opNum >= 0; opNum--) {
            BackStackRecord.Op op = transaction.mOps.get(opNum);
            addToFirstInLastOut(transaction, op, transitioningFragments, true, isReordered);
        }
    }

    public static boolean supportsTransition() {
        return (PLATFORM_IMPL == null && SUPPORT_IMPL == null) ? false : true;
    }

    private static void addToFirstInLastOut(BackStackRecord transaction, BackStackRecord.Op op, SparseArray<FragmentContainerTransition> transitioningFragments, boolean isPop, boolean isReorderedTransaction) {
        int containerId;
        boolean wasAdded;
        boolean setFirstOut;
        boolean wasRemoved;
        boolean setLastIn;
        FragmentContainerTransition containerTransition;
        FragmentContainerTransition containerTransition2;
        Fragment fragment;
        FragmentContainerTransition containerTransition3;
        FragmentContainerTransition containerTransition4;
        boolean setLastIn2;
        boolean setFirstOut2;
        boolean setFirstOut3;
        boolean setLastIn3;
        Fragment fragment2 = op.fragment;
        if (fragment2 == null || (containerId = fragment2.mContainerId) == 0) {
            return;
        }
        int command = isPop ? INVERSE_OPS[op.cmd] : op.cmd;
        boolean z = false;
        switch (command) {
            case 1:
            case 7:
                if (isReorderedTransaction) {
                    setLastIn2 = fragment2.mIsNewlyAdded;
                } else {
                    if (!fragment2.mAdded && !fragment2.mHidden) {
                        z = true;
                    }
                    setLastIn2 = z;
                }
                setLastIn = setLastIn2;
                wasRemoved = false;
                setFirstOut = false;
                wasAdded = true;
                break;
            case 2:
            default:
                setLastIn = false;
                wasRemoved = false;
                setFirstOut = false;
                wasAdded = false;
                break;
            case 3:
            case 6:
                if (isReorderedTransaction) {
                    if (!fragment2.mAdded && fragment2.mView != null && fragment2.mView.getVisibility() == 0 && fragment2.mPostponedAlpha >= 0.0f) {
                        z = true;
                    }
                    setFirstOut2 = z;
                } else {
                    if (fragment2.mAdded && !fragment2.mHidden) {
                        z = true;
                    }
                    setFirstOut2 = z;
                }
                setLastIn = false;
                wasRemoved = true;
                setFirstOut = setFirstOut2;
                wasAdded = false;
                break;
            case 4:
                if (isReorderedTransaction) {
                    if (fragment2.mHiddenChanged && fragment2.mAdded && fragment2.mHidden) {
                        z = true;
                    }
                    setFirstOut3 = z;
                } else {
                    if (fragment2.mAdded && !fragment2.mHidden) {
                        z = true;
                    }
                    setFirstOut3 = z;
                }
                setLastIn = false;
                wasRemoved = true;
                setFirstOut = setFirstOut3;
                wasAdded = false;
                break;
            case 5:
                if (isReorderedTransaction) {
                    if (fragment2.mHiddenChanged && !fragment2.mHidden && fragment2.mAdded) {
                        z = true;
                    }
                    setLastIn3 = z;
                } else {
                    setLastIn3 = fragment2.mHidden;
                }
                setLastIn = setLastIn3;
                wasRemoved = false;
                setFirstOut = false;
                wasAdded = true;
                break;
        }
        FragmentContainerTransition containerTransition5 = transitioningFragments.get(containerId);
        if (!setLastIn) {
            containerTransition = containerTransition5;
        } else {
            FragmentContainerTransition containerTransition6 = ensureContainer(containerTransition5, transitioningFragments, containerId);
            containerTransition6.lastIn = fragment2;
            containerTransition6.lastInIsPop = isPop;
            containerTransition6.lastInTransaction = transaction;
            containerTransition = containerTransition6;
        }
        if (isReorderedTransaction || !wasAdded) {
            fragment = null;
            containerTransition2 = containerTransition;
        } else {
            if (containerTransition != null && containerTransition.firstOut == fragment2) {
                containerTransition.firstOut = null;
            }
            FragmentManagerImpl manager = transaction.mManager;
            if (fragment2.mState >= 1 || manager.mCurState < 1 || transaction.mReorderingAllowed) {
                fragment = null;
                containerTransition2 = containerTransition;
            } else {
                manager.makeActive(fragment2);
                containerTransition2 = containerTransition;
                fragment = null;
                manager.moveToState(fragment2, 1, 0, 0, false);
            }
        }
        if (setFirstOut) {
            containerTransition4 = containerTransition2;
            if (containerTransition4 == null || containerTransition4.firstOut == null) {
                containerTransition3 = ensureContainer(containerTransition4, transitioningFragments, containerId);
                containerTransition3.firstOut = fragment2;
                containerTransition3.firstOutIsPop = isPop;
                containerTransition3.firstOutTransaction = transaction;
                if (isReorderedTransaction && wasRemoved && containerTransition3 != null && containerTransition3.lastIn == fragment2) {
                    containerTransition3.lastIn = fragment;
                    return;
                }
                return;
            }
        } else {
            containerTransition4 = containerTransition2;
        }
        containerTransition3 = containerTransition4;
        if (isReorderedTransaction) {
        }
    }

    private static FragmentContainerTransition ensureContainer(FragmentContainerTransition containerTransition, SparseArray<FragmentContainerTransition> transitioningFragments, int containerId) {
        if (containerTransition == null) {
            FragmentContainerTransition containerTransition2 = new FragmentContainerTransition();
            transitioningFragments.put(containerId, containerTransition2);
            return containerTransition2;
        }
        return containerTransition;
    }

    /* loaded from: classes.dex */
    public static class FragmentContainerTransition {
        public Fragment firstOut;
        public boolean firstOutIsPop;
        public BackStackRecord firstOutTransaction;
        public Fragment lastIn;
        public boolean lastInIsPop;
        public BackStackRecord lastInTransaction;

        FragmentContainerTransition() {
        }
    }

    private FragmentTransition() {
    }
}
