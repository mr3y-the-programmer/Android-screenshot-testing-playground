<a href="https://androidweekly.net/issues/issue-479">
<img src="https://androidweekly.net/issues/issue-479/badge">
</a>

# Road to effective snapshot testing </br>
A sample repo describing best practices for snapshot testing on Android. This includes for now:
1. **Parameterized Tests**: Write the test once and run it under all desired configurations. This includes snapshot tests under various **Font sizes**
2. **Filtered Parameterized Tests**: Create filters to group the configurations, so that we can run only the tests corresponding to a certain group. As described later in this ReadMe, this can be useful when having a large bunch of snapshot tests, in order to reduce building times. That way we can select to run a group of them on every PR (e.g. the essential ones), and the rest once a day.

The code here will reflect everything discussed on my series on [snapshot testing](https://sergiosastre.hashnode.dev/an-introduction-to-snapshot-testing-on-android-in-2021)

All the samples use [Shot from Karumi](https://github.com/Karumi/Shot), what is a great library that facilitates
snasphot testing.
I've been using an emulator with API 23 for a matter of simplicity. That is because `Shot` requires less
configuration for API < 29.

## Parameterized Snapshot Tests
They enable us to test a given view with all possible different configurations, namely:
  1. Theme (Light and Dark)
  2. Font Size
  3. Languages
  4. View dimensions (width and height)
  5. Specific view states
  6. Others...

With Parameterized snapshot test we can write the test once and run it for all the desired configurations!
This can be achieved by using `org.junit.runners.Parameterized` as in the example below.

1. Create a `TestItem` which encapsulates all the info we want to inject as parameter. I strongly
recommend to add a testName to it, which must be unique among all tests. This is used to distinguish
each snapshot file from the rest.
```kotlin
class TestItem(
    val fontScale: FontScale,
    val theme: DialogTheme,
    val texts: Array<Int>,
    val testName: String
)
```

2. Write the method that inflates the view and takes/compares the screenshot. Use the parameters from
`TestItem` to set the view into the right state.
```kotlin
private fun ScreenshotTest.snapDeleteDialog(testItem: TestItem) {
    val activity = launch(MainActivity::class.java)
        .waitForActivity(testItem.theme.themId)

    val dialog = waitForView {
        DialogBuilder.buildDeleteDialog(
            activity,
            onDeleteClicked = {/* no-op*/ },
            bulletTexts = itemArray(testItem.texts)
        )
    }

    compareScreenshot(dialog, name = testItem.testName, widthInPx = 800)
}
```
If you also want to enable different `Font size` configurations to your tests, you can add the
[FontSizeTestRule](https://github.com/sergio-sastre/FontSizeTestRule) library to your project. We will
see in the next section how to use it.

3. Finally, we need to add the parameters. We will use `org.junit.runners.Parameterized` for that.
We will pass the `TestItem` in the class constructor, and define each single configuration to be run
inside a method annotated with `@Parameterized.Parameters`.

```kotlin
@RunWith(Parameterized::class)
class AllDeleteDialogSnapshotTest(private val testItem: TestItem) : ScreenshotTest {

    @get:Rule
    val fontSize = FontScaleRules.fontScaleTestRule(testItem.fontScale)

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Array<TestItem> =
            arrayOf(
                TestItem(
                    SMALL,
                    MATERIAL_DARK,
                    arrayOf(R.string.shortest),
                    "DARK_SMALL"
                ),
                TestItem(
                    HUGE,
                    MATERIAL_DARK,
                    repeatedItem(7, R.string.largest),
                    "DARK_HUGE"
                )
            )
    }

    @Test
    fun snapDialog() {
        snapDeleteDialog(testItem)
    }
}

```
Now you can just run your test with `./gradlew executeScreenshotTests -Precord` and you should see the
results!

## Filter Parameterized Tests
Running snapshot tests for all configurations on every PR can be very time-consuming and lead to
incrementally slower builds. One approach to solve that issue is to run only a part of those tests
on every PR (e.g. the most common config, our "smoke" tests) and all snapshot tests - or "all non-smoke
tests" - once a day (e.g. during the night or whenever it disturbs your team the least).
In doing so, we get informed of the most important visual regression bugs on every PR (i.e. blocking bugs),
and still get notified of the non-blocking bugs once a day.

We can accomplish this by filtering our tests with
`-Pandroid.testInstrumentationRunnerArguments.annotation=com.your.package.YourAnnotation`
or
`-Pandroid.testInstrumentationRunnerArguments.notAnnotation=com.your.package.YourAnnotation`

**Warning**: These arguments are supported by `org.junit.runners.Parameterized`, but not by all runners,
e.g. `JUnitParams` fails if used.

So first of all, we need to create our annotation
```kotlin
annotation class SmokeTest
```

And then we add another Test Class that reuses our previously defined `TestItem` and `snapDeleteDialog(testItem)`
containing the "most common config" parameters, like here below
```kotlin
@RuWith(Parameterized::class)
class BasicDeleteDialogSnapshotTest(private val testItem: TestItem) : ScreenshotTest {

    @get:Rule
    val fontSize = FontScaleRules.fontScaleTestRule(testItem.fontScale)

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Array<TestItem> = arrayOf(
            TestItem(
                NORMAL,
                MATERIAL_LIGHT,
                arrayOf(R.string.largest, R.string.middle, R.string.shortest),
                "SMOKE"
            )
        )
    }

    @SmokeTest
    @Test
    fun snapDialog() {
        snapDeleteDialog(testItem)
    }
}
```

Take a look at [DelegateDialogTest.kt](https://github.com/sergio-sastre/RoadToEffectiveSnapshotTesting/blob/master/app/src/androidTest/java/com/example/road/to/effective/snapshot/testing/parameterized/DeleteDialogTest.kt) to see how these Parameterized Tests are implemented and run
`./gradlew executeScreenshotTests -Precord -Pandroid.testInstrumentationRunnerArguments.annotation=com.example.road.to.effective.snapshot.testing.utils.SmokeTest`
to verify that only the `@SmokeTest` runs!

## What is coming next:
1. More advance samples
2. Tips to remove flakiness
3. Tips to increase test execution speed
4. Jetpack compose samples
and more...


