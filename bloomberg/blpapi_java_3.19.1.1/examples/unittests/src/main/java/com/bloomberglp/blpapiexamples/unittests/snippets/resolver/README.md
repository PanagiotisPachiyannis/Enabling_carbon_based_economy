
# Resolver Test Examples

There are several interactions that are particular to resolvers:
 - Resolver service registration
 - Permission requests

## Mocking ProviderSession

A mocked `ProviderSession` can then be used in place of a real instance to
set expectations.

For example, given an event handler, `MyEventHandler`, it can be tested as
the following:

```java
@Mock
ProvierSession mockSession;

@Test
public void firstTest() {
    MyEventHandler testHandler;
    Event testEvent; // See next section

    // Setup mock
    when(mockSession.registerService(
                    anyString(),
                    any(Identity.class),
                    any(ServiceRegistrationOptions.class)))
            .thenReturn(true);

    testHandler.processEvent(testEvent, mockSession);

    // Verify mock
    verify(mockSession).registerService(
                    anyString(),
                    any(Identity.class),
                    any(ServiceRegistrationOptions.class));
}
```

## Creating Test Events

In order to be able to test `EventHandler`s or the output of
`session.nextEvent()`, the application should be able to generate custom
test events / messages.

Some samples are provided in this package to demonstrate how to generate
all possible admin messages.
