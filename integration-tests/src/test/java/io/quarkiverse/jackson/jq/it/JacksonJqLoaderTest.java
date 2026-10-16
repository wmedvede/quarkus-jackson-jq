package io.quarkiverse.jackson.jq.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
public class JacksonJqLoaderTest {

    @Test
    public void jqFunctionsTest() {
        given().when()
                .contentType(ContentType.JSON)
                .get("/jq/jqfunctions")
                .then()
                .statusCode(200)
                .body("size()", greaterThan(0))
                .body("$", hasItem("@json/0"))
                .body("$", hasItem("@text/0"))
                .body("$", hasItem("_flatten/1"))
                .body("$", hasItem("_modify/2"))
                .body("$", hasItem("walk/1"))
                .body("$", hasItem("while/2"))
                .body("$", hasItem("with_entries/1"));
    }

    @Test
    public void functionsTest() {
        // functions = [@base64/0, @base64d/0, @csv/0, @html/0, @sh/0, @tsv/0, @uri/0, acos/0, asin/0, atan/0, atan2/2, builtins/0, cbrt/0, ceil/0, contains/1, cos/0, cosh/0, delpaths/1, empty/0, endswith/1, error/0, error/1, exp/0, exp10/0, exp2/0, explode/0, expm1/0, floor/0, from_entries/0, fromdateiso8601/0, fromjson/0, getpath/1, group_by/1, has/1, implode/0, index/1, indices/1, infinite/0, isempty/1, isinfinite/0, isnan/0, isnormal/0, join/1, keys/0, keys_unsorted/0, length/0, log/0, log10/0, log1p/0, log2/0, max_by/1, min_by/1, myFunctionRuntime/2, myFunctionStatic/2, nan/0, not/0, now/0, path/1, paths/1, pow/2, range/1, range/2, range/3, reverse/0, rindex/1, round/0, setpath/2, sin/0, sinh/0, sort_by/1, split/1, sqrt/0, startswith/1, tan/0, tanh/0, to_entries/0, todateiso8601/0, tojson/0, tonumber/0, tostring/0, type/0, utf8bytelength/0]
        given().when()
                .contentType(ContentType.JSON)
                .get("/jq/functions")
                .then()
                .statusCode(200)
                .body("size()", greaterThan(0))
                //                .body("$", not(hasItem("min_by/1"))) // built-in function filtered
                .body("$", hasItem("paths/1")) // built-in function not filtered
                .body("$", hasItem("delpaths/1")) // built-in function not filtered

                .body("$", hasItem("myGreet/1")) // custom function not filtered
                .body("$", hasItem("myGreet/2")); // custom function not filtered

        //                .body("$", hasItem("strftime/1")) // extra function not filtered
        //                .body("$", hasItem("strftime/2")) // extra function not filtered
        //                .body("$", hasItem("uridecode/0")) // extra function not filtered
        //                .body("$", hasItem("uriparse/0")) // extra function not filtered

        //                .body("$", hasItem("entry.getKey().name() + \"/\" + entry.getKey().arity()/1")) // extra function not filtered

        //                .body("$", hasItem("strptime/2")) // extra function not filtered
        //                .body("$", hasItem("timestamp/0")) // extra function not filtered
        //                .body("$", hasItem("uuid4/0")); // extra function not filtered
    }
}
