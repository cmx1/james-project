/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.jmap.methods.integration;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.with;
import static com.jayway.restassured.config.EncoderConfig.encoderConfig;
import static com.jayway.restassured.config.RestAssuredConfig.newConfig;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsMapWithSize.aMapWithSize;

import org.apache.james.GuiceJamesServer;
import org.apache.james.jmap.JmapAuthentication;
import org.apache.james.jmap.api.access.AccessToken;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;

public abstract class SetMailboxesMethodTest {

    private static final String NAME = "[0][0]";
    private static final String ARGUMENTS = "[0][1]";
    private static final String USERS_DOMAIN = "domain.tld";

    protected abstract GuiceJamesServer<?> createJmapServer();

    protected abstract void await();

    private AccessToken accessToken;
    private String username;
    private GuiceJamesServer<?> jmapServer;

    @Before
    public void setup() throws Throwable {
        jmapServer = createJmapServer();
        jmapServer.start();
        RestAssured.port = jmapServer.getJmapPort();
        RestAssured.config = newConfig().encoderConfig(encoderConfig().defaultContentCharset(Charsets.UTF_8));
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        username = "username@" + USERS_DOMAIN;
        String password = "password";
        jmapServer.serverProbe().addDomain(USERS_DOMAIN);
        jmapServer.serverProbe().addUser(username, password);
        jmapServer.serverProbe().createMailbox("#private", username, "inbox");
        accessToken = JmapAuthentication.authenticateJamesUser(username, password);

        await();
    }

    @After
    public void teardown() {
        jmapServer.stop();
    }

    @Test
    public void setMailboxesShouldErrorNotSupportedWhenRoleGiven() throws Exception {
        String requestBody =
            "[" +
                "  [ \"setMailboxes\"," +
                "    {" +
                "      \"create\": {" +
                "        \"create-id01\" : {" +
                "          \"name\" : \"foo\"," +
                "          \"role\" : \"Inbox\"" +
                "        }" +
                "      }" +
                "    }," +
                "    \"#0\"" +
                "  ]" +
                "]";

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", this.accessToken.serialize())
            .body(requestBody)
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("error"))
            .body(ARGUMENTS + ".type", equalTo("Not yet implemented"));
    }

    @Test
    public void setMailboxesShouldErrorNotSupportedWhenSortOrderGiven() throws Exception {
        String requestBody =
            "[" +
                "  [ \"setMailboxes\"," +
                "    {" +
                "      \"create\": {" +
                "        \"create-id01\" : {" +
                "          \"name\" : \"foo\"," +
                "          \"sortOrder\" : 11" +
                "        }" +
                "      }" +
                "    }," +
                "    \"#0\"" +
                "  ]" +
                "]";

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", this.accessToken.serialize())
            .body(requestBody)
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("error"))
            .body(ARGUMENTS + ".type", equalTo("Not yet implemented"));
    }

    @Test
    public void setMailboxesShouldReturnCreatedMailbox() throws Exception {
        String requestBody =
            "[" +
                "  [ \"setMailboxes\"," +
                "    {" +
                "      \"create\": {" +
                "        \"create-id01\" : {" +
                "          \"name\" : \"foo\"" +
                "        }" +
                "      }" +
                "    }," +
                "    \"#0\"" +
                "  ]" +
                "]";

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", this.accessToken.serialize())
            .body(requestBody)
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("mailboxesSet"))
            .body(ARGUMENTS + ".created", hasKey("create-id01"));
    }

    @Test
    public void setMailboxesShouldCreateMailbox() throws Exception {
        String requestBody =
            "[" +
                "  [ \"setMailboxes\"," +
                "    {" +
                "      \"create\": {" +
                "        \"create-id01\" : {" +
                "          \"name\" : \"foo\"" +
                "        }" +
                "      }" +
                "    }," +
                "    \"#0\"" +
                "  ]" +
                "]";

        with()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", this.accessToken.serialize())
            .body(requestBody)
            .then()
            .post("/jmap");

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", this.accessToken.serialize())
            .body("[[\"getMailboxes\", {}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("mailboxes"))
            .body(ARGUMENTS + ".list", hasSize(2))
            .body(ARGUMENTS + ".list.name", hasItems("foo"));
    }

    @Test
    public void setMailboxesShouldReturnCreatedMailboxWhenChildOfInboxMailbox() throws Exception {
        String inboxId =
            with()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .header("Authorization", this.accessToken.serialize())
                .body("[[\"getMailboxes\", {}, \"#0\"]]")
            .when()
                .post("/jmap")
            .then()
                .extract()
                .jsonPath()
                .getString(ARGUMENTS + ".list[0].id");

        String requestBody =
            "[" +
                "  [ \"setMailboxes\"," +
                "    {" +
                "      \"create\": {" +
                "        \"create-id01\" : {" +
                "          \"name\" : \"foo\"," +
                "          \"parentId\" : \"" + inboxId + "\"" +
                "        }" +
                "      }" +
                "    }," +
                "    \"#0\"" +
                "  ]" +
                "]";

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", this.accessToken.serialize())
            .body(requestBody)
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("mailboxesSet"))
            .body(ARGUMENTS + ".created", aMapWithSize(1))
            .body(ARGUMENTS + ".created", hasEntry(equalTo("create-id01"), Matchers.allOf(
                    hasEntry(equalTo("parentId"), equalTo(inboxId)),
                    hasEntry(equalTo("name"), equalTo("foo")))));
    }

    @Test
    public void setMailboxesShouldCreateMailboxWhenChildOfInboxMailbox() throws Exception {
        String inboxId =
            with()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .header("Authorization", this.accessToken.serialize())
                .body("[[\"getMailboxes\", {}, \"#0\"]]")
            .when()
                .post("/jmap")
            .then()
                .extract()
                .jsonPath()
                .getString(ARGUMENTS + ".list[0].id");

        String requestBody =
            "[" +
                "  [ \"setMailboxes\"," +
                "    {" +
                "      \"create\": {" +
                "        \"create-id01\" : {" +
                "          \"name\" : \"foo\"," +
                "          \"parentId\" : \"" + inboxId + "\"" +
                "        }" +
                "      }" +
                "    }," +
                "    \"#0\"" +
                "  ]" +
                "]";

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", this.accessToken.serialize())
            .body(requestBody)
        .when()
            .post("/jmap");

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", this.accessToken.serialize())
            .body("[[\"getMailboxes\", {}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("mailboxes"))
            .body(ARGUMENTS + ".list", hasSize(2))
            .body(ARGUMENTS + ".list.name", hasItems("foo"));
    }

    @Test
    public void setMailboxesShouldReturnNotCreatedWhenInvalidParentId() throws Exception {
        String requestBody =
            "[" +
                "  [ \"setMailboxes\"," +
                "    {" +
                "      \"create\": {" +
                "        \"create-id01\" : {" +
                "          \"name\" : \"foo\"," +
                "          \"parentId\" : \"123\"" +
                "        }" +
                "      }" +
                "    }," +
                "    \"#0\"" +
                "  ]" +
                "]";

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", this.accessToken.serialize())
            .body(requestBody)
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("mailboxesSet"))
            .body(ARGUMENTS + ".notCreated", aMapWithSize(1))
            .body(ARGUMENTS + ".notCreated", hasEntry(equalTo("create-id01"), Matchers.allOf(
                    hasEntry(equalTo("type"), equalTo("invalidArguments")),
                    hasEntry(equalTo("description"), equalTo("The parent mailbox '123' was not found.")))));
    }

    @Test
    public void setMailboxesShouldReturnCreatedMailboxWhenCreatingParentThenChildMailboxes() throws Exception {
        String requestBody =
            "[" +
                "  [ \"setMailboxes\"," +
                "    {" +
                "      \"create\": {" +
                "        \"create-id00\" : {" +
                "          \"name\" : \"parent\"" +
                "        }," +
                "        \"create-id01\" : {" +
                "          \"name\" : \"child\"," +
                "          \"parentId\" : \"create-id00\"" +
                "        }" +
                "      }" +
                "    }," +
                "    \"#0\"" +
                "  ]" +
                "]";

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", this.accessToken.serialize())
            .body(requestBody)
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("mailboxesSet"))
            .body(ARGUMENTS + ".created", aMapWithSize(2))
            .body(ARGUMENTS + ".created", hasEntry(equalTo("create-id00"), Matchers.allOf(
                    hasEntry(equalTo("parentId"), isEmptyOrNullString()),
                    hasEntry(equalTo("name"), equalTo("parent")))))
            .body(ARGUMENTS + ".created", hasEntry(equalTo("create-id01"), Matchers.allOf(
                    hasEntry(equalTo("parentId"), not(isEmptyOrNullString())),
                    hasEntry(equalTo("name"), equalTo("child")))));
    }

    @Test
    public void setMailboxesShouldReturnCreatedMailboxWhenCreatingChildThenParentMailboxes() throws Exception {
        String requestBody =
            "[" +
                "  [ \"setMailboxes\"," +
                "    {" +
                "      \"create\": {" +
                "        \"create-id01\" : {" +
                "          \"name\" : \"child\"," +
                "          \"parentId\" : \"create-id00\"" +
                "        }," +
                "        \"create-id00\" : {" +
                "          \"name\" : \"parent\"" +
                "        }" +
                "      }" +
                "    }," +
                "    \"#0\"" +
                "  ]" +
                "]";

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", this.accessToken.serialize())
            .body(requestBody)
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("mailboxesSet"))
            .body(ARGUMENTS + ".created", aMapWithSize(2))
            .body(ARGUMENTS + ".created", hasEntry(equalTo("create-id00"), Matchers.allOf(
                    hasEntry(equalTo("parentId"), isEmptyOrNullString()),
                    hasEntry(equalTo("name"), equalTo("parent")))))
            .body(ARGUMENTS + ".created", hasEntry(equalTo("create-id01"), Matchers.allOf(
                    hasEntry(equalTo("parentId"), not(isEmptyOrNullString())),
                    hasEntry(equalTo("name"), equalTo("child")))));
    }

    @Test
    public void setMailboxesShouldReturnNotCreatedWhenMailboxAlreadyExists() throws Exception {
        jmapServer.serverProbe().createMailbox("#private", username, "myBox");
        String requestBody =
            "[" +
                "  [ \"setMailboxes\"," +
                "    {" +
                "      \"create\": {" +
                "        \"create-id01\" : {" +
                "          \"name\" : \"myBox\"" +
                "        }" +
                "      }" +
                "    }," +
                "    \"#0\"" +
                "  ]" +
                "]";

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", this.accessToken.serialize())
            .body(requestBody)
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("mailboxesSet"))
            .body(ARGUMENTS + ".notCreated", aMapWithSize(1))
            .body(ARGUMENTS + ".notCreated", hasEntry(equalTo("create-id01"), Matchers.allOf(
                    hasEntry(equalTo("type"), equalTo("invalidArguments")),
                    hasEntry(equalTo("description"), equalTo("The mailbox 'create-id01' already exists.")))));
    }

    @Test
    public void setMailboxesShouldReturnNotCreatedWhenCycleDetected() throws Exception {
        String requestBody =
            "[" +
                "  [ \"setMailboxes\"," +
                "    {" +
                "      \"create\": {" +
                "        \"create-id01\" : {" +
                "          \"name\" : \"A\"," +
                "          \"parentId\" : \"create-id02\"" +
                "        }," +
                "        \"create-id02\" : {" +
                "          \"name\" : \"B\"," +
                "          \"parentId\" : \"create-id01\"" +
                "        }" +
                "      }" +
                "    }," +
                "    \"#0\"" +
                "  ]" +
                "]";

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", this.accessToken.serialize())
            .body(requestBody)
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("mailboxesSet"))
            .body(ARGUMENTS + ".notCreated", aMapWithSize(2))
            .body(ARGUMENTS + ".notCreated", Matchers.allOf(
                    hasEntry(equalTo("create-id01"), Matchers.allOf(
                        hasEntry(equalTo("type"), equalTo("invalidArguments")),
                        hasEntry(equalTo("description"), equalTo("The created mailboxes introduce a cycle.")))),
                    hasEntry(equalTo("create-id02"), Matchers.allOf(
                            hasEntry(equalTo("type"), equalTo("invalidArguments")),
                            hasEntry(equalTo("description"), equalTo("The created mailboxes introduce a cycle."))))
                    ));
    }

    @Test
    public void setMailboxesShouldReturnNotCreatedWhenMailboxNameContainsPathDelimiter() throws Exception {
        String requestBody =
            "[" +
                "  [ \"setMailboxes\"," +
                "    {" +
                "      \"create\": {" +
                "        \"create-id01\" : {" +
                "          \"name\" : \"A.B.C.D\"" +
                "        }" +
                "      }" +
                "    }," +
                "    \"#0\"" +
                "  ]" +
                "]";

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", this.accessToken.serialize())
            .body(requestBody)
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("mailboxesSet"))
            .body(ARGUMENTS + ".notCreated", aMapWithSize(1))
            .body(ARGUMENTS + ".notCreated", hasEntry(equalTo("create-id01"), Matchers.allOf(
                        hasEntry(equalTo("type"), equalTo("invalidArguments")),
                        hasEntry(equalTo("description"), equalTo("The mailbox 'A.B.C.D' contains an illegal character: '.'")))
                    ));
    }
}