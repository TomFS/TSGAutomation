import io.restassured.http.ContentType;
import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static io.restassured.http.ContentType.JSON;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;


public class BaseTest {
    // Base API Address - Ideally moved into config file
    private String BaseUrl = "http://localhost:3000";
    private int testFixtureId = 0;

    private String testFixture(int id) {
        return "{\"fixtureId\": \"" + id + "\", \"fixtureStatus\":{ \"displayed\":true, \"suspended\": false}," +
                "\"footballFullState\":{\"homeTeam\":\"Test Home Team\",\"awayTeam\":\"Test Away Team\",\"finished\":true,\"gameTimeInSeconds\":5400," +
                "\"goals\":[],\"period\":\"\",\"possibles\":[],\"corners\":[],\"redCards\":[],\"yellowCards\":[]," +
                "\"startDateTime\":\"2019-08-20T14:00:00\",\"started\":true," +
                "\"teams\":[{\"association\":\"HOME\",\"name\":\"Test Home Team\",\"teamId\": \"HOME\"}," +
                "{\"association\":\"AWAY\",\"name\":\"Test Away Team\",\"teamId\": \"AWAY\"}]}}";
    }

    private int createFixture(int id) {
        return given().contentType(ContentType.JSON).body(testFixture(id))
                .when().post(BaseUrl + "/fixture")
                .then().extract().statusCode();
    }

    @Before
    public void setup() {
        Awaitility.reset();
        Awaitility.setDefaultPollDelay(100, MILLISECONDS);
        Awaitility.setDefaultPollInterval(100, MILLISECONDS);
        Awaitility.setDefaultTimeout(10, SECONDS);
    }

    @After
    public void tearDown() {
        if (testFixtureId != 0) {
            when().delete(BaseUrl + "/fixture/" + testFixtureId);
        }
    }

    @Test
    // Get all available fixtures. Assert that 3 are returned, all with a valid value for 'fixtureId'
    public void test1_GetAllFixtures() {
        when().get(BaseUrl + "/fixtures")
                .then().statusCode(200).contentType(JSON)
                .body("findall.size()", equalTo(3))
                .body("[0].fixtureId", notNullValue())
                .body("[1].fixtureId", notNullValue())
                .body("[2].fixtureId", notNullValue())
                .log().ifValidationFails();
    }

    @Test
    // Store a new fixture in the database. Get the new fixture and assert that the first object in Teams array has a teamId of 'HOME'.
    public void test2_CreateNewFixtureAndAssertTeamId() {
        testFixtureId = 4;
        // Should ideally store test fixure in DB directly rather than using API
        await().untilAsserted(() -> assertThat(createFixture(testFixtureId), equalTo(200)));

        when().get(BaseUrl + "/fixture/4")
                .then().statusCode(200).contentType(JSON)
                .body("fixtureId", equalTo(Integer.toString(testFixtureId)))
                .body("footballFullState.teams[0].teamId", equalTo("HOME"))
                .log().ifValidationFails();
    }

    @Test
    // Create a new fixture and then retrieve it as soon as it's available, accounting for any delay in creation
    public void test3_CreateFixtureAndRetrieveAsSoonAsAvailable() throws InterruptedException {
        testFixtureId = 5;
        // Rest Assured never seems to have any issue with response delay, but added Awaitility to ensure handling
        await().untilAsserted(() -> assertThat(createFixture(testFixtureId), equalTo(200)));

        System.out.print("Getting fixture: " + testFixtureId);
        given().when().get(BaseUrl + "/fixture/" + testFixtureId)
                .then().statusCode(200).contentType(JSON).log().ifValidationFails();
    }

    @Test
    // Create a new fixture, then delete it and assert that it no longer exists
    public void test4_CreateThenDeleteFixtureAndCheckRemoval() {
        testFixtureId = 6;
        await().untilAsserted(() -> assertThat(createFixture(testFixtureId), equalTo(200)));

        when().delete(BaseUrl + "/fixture/" + testFixtureId)
                .then().statusCode(200).log().ifValidationFails();

        when().get(BaseUrl + "/fixture/" + testFixtureId)
                .then().statusCode(404).log().ifValidationFails();
    }
}


