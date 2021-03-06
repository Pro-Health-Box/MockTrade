# PresenterActivity in Android MVP

There are almost as many Android MVP patterns as there are Android
applications. Most Android MVP Architectures implement the View logic in
the Activity. This doc makes the case for putting the view logic in
custom view classes and use the Activity as the Presenter.

### TL;DR
- **PresenterActivity** - Activity base class encapsulating presenter functionality
    - Responds to Android Lifecycle events, gets data from Model, sends data to View
    - Handles View events, fetches or retrieves data from Model, handle app naviation
- **View** - View logic encapsulated into custom view classes
    - Reusable across Activities and Fragments
    - Exposes listener interface to be implemented by the presenter
- **Model** - Data API and access classes
    - **Model Objects** - POJOs for application specific entities
    - **ModelProvider** - application scoped data access classes used for DI
    - **Model API** - fetches and persists **Model Objects** to a specific data store using
       objects from the **ModelProvider**

### Table of Contents
- [Other Android MVP Approaches](#other-android-mvp-approaches)
    - [Mosby MVP](#mosby-mvp)
    - [GoogleSamples todo-mvp](#googlesamples-todo-mvp)
- [PresenterActivity](#presenteractivity)
- [Model and ModelProvider](#model-and-modelprovider)
    - [Model Objects](#model-objects)
    - [ModelProvider](#modelprovider)
    - [Model API](#model-api)
- [View](#view)
- [Unit Testing](#unit-testing)
- [Conclusion](#conclusion)

### Sample Implementations:
* [MockTrade](https://github.com/balch/MockTrade)
* [EBay Auction Browser](https://github.com/balch/AuctionBrowser)

### Other Android MVP Approaches

#### [Mosby MVP](http://hannesdorfmann.com/mosby/mvp/)

Mosby is an interesting Android MVP Framework that has a lot of cool
functionality. I like that version 2.0 is leaner and meaner, and the
ViewState and Loading-Content-Error features show they understand the
complexities of developing Android applications.

I found this quote which nicely describes their MVP philosophy:
> But before we dive deeper in how to implement MVP on Android we have
> to clarify if an Activity or Fragment is a View or a Presenter.
> Activity and Fragment seems to be both, because they have lifecycle
> callbacks like onCreate() or onDestroy() as well as responsibilities
> of View things like switching from one UI widget to another UI widget
> (like showing a ProgressBar while loading and then displaying a
> ListView with data). You may say that these sounds like an Activity or
> Fragment is a Controller. However, we came to the conclusion that
> Activity and Fragment should be treated as part of a (dumb) View and
> not as a Presenter. You will see why afterwards.

This quote asks the right question, but comes up with a different
answer. They are correct that the Activity/Fragment provide both
Presenter and View methods and behavior, but, I would argue that the
View methods (mainly `.findViewById()`) are convenience methods that are
most useful for creating Android sample applications.

The `Activity.setContentView(View view)` is the primary example I use to
make the case that the view is conceptually separate from the Activity.
The View is passed to the Activity and held as a reference (by adding it
to the Window object). Calls to `Activity.findViewById()` are delegated
to the stored View (since both View and Activity implement
`.findViewById()`). This same concept also applies to Fragments and is
reinforced by the `Fragment.onCreateView()` method which forces the
Fragment derived class to create a separate view.

Storing another reference to the View in a separate Presenter class adds
another level of indirection: Activity (Lifecycle Events) --&gt;
Presenter --&gt; View (which is actually the Activity). I first tried
this approach in an application using complex Lifecycle Events tied to
Loaders and `onActivityResult()`. I found the Presenter eventually
morphed into a subset of Activity Lifecycle Events codified into an
interface.

#### [GoogleSamples todo-mvp](https://github.com/googlesamples/android-architecture/tree/todo-mvp/)

The GoogleSamples repo demonstrates another flavor of the MVP
architecture where view logic is implemented in the Fragment. The
interesting part of this pattern is having the View and Presenter
interfaces defined in a Contract interface.

```java
// This specifies the contract between the view and the presenter.
public interface TasksContract {
    interface View extends BaseView<Presenter> {
        void setLoadingIndicator(boolean active);
        void showTasks(List<Task> tasks);
        void showAddTask();
        ...
    }

    interface Presenter extends BasePresenter {
        void result(int requestCode, int resultCode);
        void loadTasks(boolean forceUpdate);
        void addNewTask();
        ...
    }
}
```

I like the concept of a Presenter contract, but have implemented in a
slightly different location in the PresenterActivity pattern. Each View
class defines an interface which must be implemented by the caller. The
interface allows the Presenter to handle events generated by the View
and is used to drive the View.

This pattern is demonstrated in code samples in the [View](#view)
section.

### PresenterActivity

The Activity Lifecycle is the heart of any Android Application. It
provides access to important events that need to be handled to create a
robust Android Application. In addition to the common activity events
(onCreate/OnDestroy, onStart/onPause, onResume/onPause), there are
advanced events (onActivityResult, onSaveInstanceState) which must be
handled in most Android Applications.

When implemented separately from the Activity, a typical Presenter class
usually contains methods that correspond directly to LifeCycle events.
In addition, the Presenter base interfaces usually define a subset of
the Lifecycle methods which limits the Application in certain
circumstances.

The **PresenterActivity** solves the problem in a unique way. The
supported Activity LifeCycle methods are wrapped and exposed to the
Child class by using a `Base` naming convention.

**[Full PresenterActivity.java Source](https://github.com/balch/MockTrade/blob/develop/AppFramework/src/main/java/com/balch/android/app/framework/PresenterActivity.java)**

```java
public abstract class PresenterActivity<V extends View & BaseView, M extends ModelProvider>
                            extends AppCompatActivity  {

    /**
     * Override abstract method to create a view of type V used by the Presenter.
     * The view id will be managed by this class  if not specified
     * @return View containing view logic in the MVP pattern
     */
    protected abstract V createView();

    /**
     * Override abstract method to create any models needed by the Presenter. A class of type
     * M is injected into this method to take advantage the Dependency Injection pattern.
     * This mechanism is implemented by requiring the Application instance be of type M.

     * @param modelProvider injected ModelProvider
     */
    protected abstract void createModel(M modelProvider);

    // override-able activity functions
    public void onCreateBase(Bundle savedInstanceState) {}
    public void onResumeBase() {}
    public void onPauseBase() {}
    public void onStartBase() {}
    public void onStopBase() {}
    public void onDestroyBase() {}
    public void onSaveInstanceStateBase(Bundle outState) {}
    public void onActivityResultBase(int requestCode, int resultCode, Intent data) { }
    public boolean onHandleException(String logMsg, Exception ex) {return false;}
    ...
}
```

This pattern requires implementers to specify **View** and
**ModelProvider** types when extending the **PresenterActivity**. The
`abstract V createView()` and `abstract void createModel(M
modelProvider)` methods enforce the **MV** part of the **MV**P pattern.
These methods are extremely useful when it comes to
[Unit Testing](#unit-testing).

The integration with the Android LifeCycle events provides a familiar
set of methods to `@Override` and makes it easy to port legacy
applications. The interaction with the LifeCycle events allows the
framework to provide integrated error handling and timing logs.

### Model and ModelProvider

The Model is the least appreciated and documented part of the MVP triad.
The Model layer has three distinct components: **Model Objects**,
**Model API**, and **ModelProvider**.

#### Model Objects

I use the old school term **Model Objects** to describe:
>a representation of meaningful real-world concepts pertinent to the
>domain that need to be modeled in software

In short, these are the POJOs that represent the data models used
throughout the application. They are persisted and retrieved by the
**Model API** layer and typically passed around to Views and Adapters to
be visually presented to the user.

#### ModelProvider

The **ModelProvider** is a simple interface to define accessors for the
application scoped objects used to persist or retrieve data.

```java
public interface AuctionModelProvider extends ModelProvider {
    Settings getSettings();
    SqlConnection getSqlConnection();
    NetworkRequestProvider getNetworkRequestProvider();
    ImageLoader getImageLoader();
}
```

The **PresenterActivity** requires the **ModelProvider** interface to be
implemented from the `Application` object. This allows the
**ModelProvider** to be passed into the `void
createModel(AuctionModelProvider modelProvider)` where the individual
components can be constructor injected into the **Model API**.

```java
public class MainActivity extends PresenterActivity<AuctionView, AuctionModelProvider>
        implements LoaderManager.LoaderCallbacks<AuctionData>, AuctionView.AuctionViewListener {
    ...
    @VisibleForTesting EBayModel auctionModel;
    @VisibleForTesting NotesModel notesModel;

    @Override
    protected void createModel(AuctionModelProvider modelProvider) {
        auctionModel = new EBayModel(getString(R.string.ebay_app_id),
                                        modelProvider.getNetworkRequest());
        notesModel = new NotesModel(modelProvider.getSqlConnection());
    }
    ...
}
```

This technique facilitates [Unit Testing](#unit-testing) by allowing the
components of the **ModelProvider** to be mocked and injected into the
test instance.

#### Model API

The **Model API** does the heavy lifting in the model layer. It&apos;s
main purpose is to persist and retrieve **Model Objects**. Data should
be transformed in this layer to map the **Model Objects** to the
underlying storage format. With the correct abstractions and Factory
pattern, the **Model API** can be implemented to allow for data sources
to be swapped out at runtime (SQL and REST implementations for example).

### View

The **View** is my favorite part of MVP, sometimes I call it the _Fluff Layer_.

The view logic in this pattern is implemented in Custom Views usually derived
from Android Layout classes.  This provides a number of reuse possibilities and is easy to include
in either a Fragment or Activity.

```java
public class MainActivity extends PresenterActivity<AuctionView, AuctionModelProvider>
       implements LoaderManager.LoaderCallbacks<AuctionData>, AuctionView.AuctionViewListener {

    @Override
    public AuctionView createView() {
        return new AuctionView(this);
    }

    @Override
    public void onCreateBase(Bundle bundle) {
        this.view.setAuctionViewListener(this);
        this.view.showBusy();
    }

    ...
}
```

Each **View** defines an interface describing the events supported by that View. The implementation
is supplied by the Presenter through a setter method.

```java
  public class AuctionView extends LinearLayout implements BaseView {
    ...
    public interface AuctionViewListener {
        boolean onLoadMore(int currentPage);
        void onChangeSort(int position);
        void onClickNoteButton(Auction auction);
        void onClickAuction(Auction auction);
        void onClickSearch(String keyword);
    }

    protected AuctionViewListener auctionViewListener;

    public AuctionView(Context context) {
        super(context);
        initializeLayout();
    }

    public AuctionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initializeLayout();
    }

    public AuctionView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initializeLayout();
    }

    private void initializeLayout() {
         inflate(getContext(), R.layout.auction_view, this);
         ...
    }

    public void setAuctionViewListener(AuctionViewListener auctionViewListener) {
        this.auctionViewListener = auctionViewListener;
    }
    ...
```

### Unit Testing

One of the stated benefits of MVP and Dependency Injection is improved
testability. This is accomplished through proper encapsulation into MVP
classes and the ability to inject alternate implementations of dependant
classes to the code being tested. This is especially powerful when
combined with a mock framework like [Mockito](http://site.mockito.org/).

The **PresenterActivity** architecture facilitates unit testing with the
minimal amount of bypassing functionality with `doNothing().when()`
calls. This is accomplished through a combination of overriding the
`createView()` method and injecting a **ModelProvider** instance with the
proper mocked application scoped objects. This technique is demonstrated
in the sample unit tests below.

The **PresenterActivity** also solves testing issues associated with the
Android framework itself. Many Android class implementations are not
available in the testing framework which causes many Activity tests to
fail with not implemented exceptions. This is typically solved through
third-party testing frameworks that have TestRunners that include a
version of the Android runtime libraries (I'm thinking of you
[Robolectric](http://robolectric.org/)).

The classes derived from **PresenterActivity** can be tested without these
frameworks because the mirrored Activity Lifecycle events do not call
their `super` counterparts. This makes writing reverse-engineered unit
test a breeze (see `testOnCreateBase()` below).

```java
public class MainActivityTest {

    @Mock AuctionView mockView;
    @Mock SqlConnection sqlConnection;
    @Mock LoaderManager loaderManager;

    private MainActivity activity;
    private AuctionModelProvider modelProvider;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        modelProvider = spy(new AuctionApplication() {
            @Override
            public SqlConnection getSqlConnection() {
                return sqlConnection;
            }
        });

        activity = spy(new MainActivity() {
            @Override
            public AuctionView createView() {
                view = mockView;
                return mockView;
            }
        });

        doReturn("").when(activity).getString(eq(R.string.ebay_app_id));
        doReturn(loaderManager).when(activity).getSupportLoaderManager();

        activity.createView();
        activity.createModel(modelProvider);
    }

    @Test
    public void testOnCreateBase() throws Exception {
        activity.onCreateBase(null);

        verify(mockView).setAuctionViewListener(eq(activity));
        verify(mockView).setSortStrings(eq(R.array.auction_sort_col));
        verify(mockView).showBusy();
        verify(loaderManager).initLoader(anyInt(), isNull(Bundle.class), eq(activity));
    }

    @Test
    public void testOnLoadMore() throws Exception {
        activity.totalPages = 5;
        activity.isLoadFinished = true;
        doNothing().when(activity).updateView();

        assertTrue(activity.onLoadMore(2));

        verify(mockView).showBusy();
        verify(activity).updateView();
    }

    @Test
    public void testOnLoadMoreNoMore() throws Exception {
        activity.totalPages = 5;
        activity.isLoadFinished = true;
        doNothing().when(activity).updateView();

        assertFalse(activity.onLoadMore(5));

        verify(mockView, never()).showBusy();
        verify(activity, never()).updateView();
    }

    @Test
    public void testSaveNote() throws Exception {
        Auction auction = mock(Auction.class);
        Note note = mock(Note.class);
        String text = "test text";

        activity.saveNote(auction, note, text);

        verify(note).setNote(eq(text));
        verify(sqlConnection).update(eq(activity.notesModel), eq(note));
    }

}
```

### Conclusion
Have I made the case for the **PresenterActivity** and changed your MVP mind yet? At the very least
I hope to have simulated some thought and discussion around my favorite pattern!

Cheers



