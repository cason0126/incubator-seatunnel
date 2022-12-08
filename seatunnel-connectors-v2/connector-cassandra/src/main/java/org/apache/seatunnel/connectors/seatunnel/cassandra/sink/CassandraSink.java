/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.seatunnel.connectors.seatunnel.cassandra.sink;

import static org.apache.seatunnel.connectors.seatunnel.cassandra.config.CassandraConfig.CQL;
import static org.apache.seatunnel.connectors.seatunnel.cassandra.config.CassandraConfig.DATACENTER;
import static org.apache.seatunnel.connectors.seatunnel.cassandra.config.CassandraConfig.FIELDS;
import static org.apache.seatunnel.connectors.seatunnel.cassandra.config.CassandraConfig.HOST;
import static org.apache.seatunnel.connectors.seatunnel.cassandra.config.CassandraConfig.KEYSPACE;
import static org.apache.seatunnel.connectors.seatunnel.cassandra.config.CassandraConfig.PASSWORD;
import static org.apache.seatunnel.connectors.seatunnel.cassandra.config.CassandraConfig.TABLE;
import static org.apache.seatunnel.connectors.seatunnel.cassandra.config.CassandraConfig.USERNAME;

import org.apache.seatunnel.api.common.PrepareFailException;
import org.apache.seatunnel.api.common.SeaTunnelAPIErrorCode;
import org.apache.seatunnel.api.sink.SeaTunnelSink;
import org.apache.seatunnel.api.sink.SinkWriter;
import org.apache.seatunnel.api.table.type.SeaTunnelDataType;
import org.apache.seatunnel.api.table.type.SeaTunnelRow;
import org.apache.seatunnel.api.table.type.SeaTunnelRowType;
import org.apache.seatunnel.common.config.CheckConfigUtil;
import org.apache.seatunnel.common.config.CheckResult;
import org.apache.seatunnel.common.constants.PluginType;
import org.apache.seatunnel.connectors.seatunnel.cassandra.client.CassandraClient;
import org.apache.seatunnel.connectors.seatunnel.cassandra.config.CassandraParameters;
import org.apache.seatunnel.connectors.seatunnel.cassandra.exception.CassandraConnectorErrorCode;
import org.apache.seatunnel.connectors.seatunnel.cassandra.exception.CassandraConnectorException;
import org.apache.seatunnel.connectors.seatunnel.common.sink.AbstractSimpleSink;
import org.apache.seatunnel.connectors.seatunnel.common.sink.AbstractSinkWriter;

import org.apache.seatunnel.shade.com.typesafe.config.Config;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ColumnDefinitions;
import com.google.auto.service.AutoService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@AutoService(SeaTunnelSink.class)
public class CassandraSink extends AbstractSimpleSink<SeaTunnelRow, Void> {

    private final CassandraParameters cassandraParameters = new CassandraParameters();
    private SeaTunnelRowType seaTunnelRowType;

    private ColumnDefinitions tableSchema;

    @Override
    public String getPluginName() {
        return "Cassandra";
    }

    @Override
    public void prepare(Config pluginConfig) throws PrepareFailException {
        CheckResult checkResult = CheckConfigUtil.checkAllExists(pluginConfig, HOST.key(), KEYSPACE.key(), CQL.key());
        if (!checkResult.isSuccess()) {
            throw new CassandraConnectorException(SeaTunnelAPIErrorCode.CONFIG_VALIDATION_FAILED,
                    String.format("PluginName: %s, PluginType: %s, Message: %s",
                            getPluginName(), PluginType.SINK, checkResult.getMsg()));
        }
        try (CqlSession session = CassandraClient.getCqlSessionBuilder(
                pluginConfig.getString(HOST.key()),
                pluginConfig.getString(KEYSPACE.key()),
                pluginConfig.getString(USERNAME.key()),
                pluginConfig.getString(PASSWORD.key()),
                pluginConfig.getString(DATACENTER.key())
        ).build()) {
            this.cassandraParameters.buildWithConfig(pluginConfig);
            List<String> fields = pluginConfig.getStringList(FIELDS.key());
            this.tableSchema = CassandraClient.getTableSchema(session, pluginConfig.getString(TABLE.key()));
            if (fields == null || fields.isEmpty()) {
                List<String> newFields = new ArrayList<>();
                for (int i = 0; i < tableSchema.size(); i++) {
                    newFields.add(tableSchema.get(i).getName().asInternal());
                }
                this.cassandraParameters.setFields(newFields);
            } else {
                for (String field : fields) {
                    if (!tableSchema.contains(field)) {
                        throw new CassandraConnectorException(CassandraConnectorErrorCode.FIELD_NOT_IN_TABLE,
                                "Field " + field + " does not exist in table " + pluginConfig.getString(TABLE.key()));
                    }
                }
            }
        } catch (Exception e) {
            throw new CassandraConnectorException(SeaTunnelAPIErrorCode.CONFIG_VALIDATION_FAILED,
                    String.format("PluginName: %s, PluginType: %s, Message: %s",
                            getPluginName(), PluginType.SINK, checkResult.getMsg()));
        }
    }

    @Override
    public void setTypeInfo(SeaTunnelRowType seaTunnelRowType) {
        this.seaTunnelRowType = seaTunnelRowType;
    }

    @Override
    public SeaTunnelDataType<SeaTunnelRow> getConsumedType() {
        return this.seaTunnelRowType;
    }

    @Override
    public AbstractSinkWriter<SeaTunnelRow, Void> createWriter(SinkWriter.Context context) throws IOException {
        return new CassandraSinkWriter(cassandraParameters, seaTunnelRowType, tableSchema);
    }
}
