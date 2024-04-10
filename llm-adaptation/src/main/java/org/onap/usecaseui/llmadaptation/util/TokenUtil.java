/*
 * Copyright (C) 2024 CMCC, Inc. and others. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onap.usecaseui.llmadaptation.util;

import com.alibaba.fastjson.JSONObject;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;

import java.util.Date;

public class TokenUtil {
    public static String generateToken(String apikey,long expSeconds) throws JOSEException {
        String[] apikeyArr=apikey.split("\\.",2);//kid.secret
        Date now=new Date();

        JSONObject payload=new JSONObject();
        payload.put("api_key",apikeyArr[0]);
        payload.put("exp",now.getTime()/1000+expSeconds);
        payload.put("timestamp",now.getTime()/1000);

        //create JWS object
        JWSObject jwsObject = new JWSObject(
                new JWSHeader.Builder(JWSAlgorithm.HS256).type(JOSEObjectType.JWT).build(),
                new Payload(payload.toJSONString()));
        jwsObject.sign(new MACSigner(apikeyArr[1]));
        return jwsObject.serialize();
    }
}
