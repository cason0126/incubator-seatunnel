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

package org.apache.seatunnel.connectors.seatunnel.file.oss.sink;

import org.apache.seatunnel.api.configuration.util.OptionRule;
import org.apache.seatunnel.api.table.factory.Factory;
import org.apache.seatunnel.api.table.factory.TableSinkFactory;
import org.apache.seatunnel.connectors.seatunnel.file.config.BaseSinkConfig;
import org.apache.seatunnel.connectors.seatunnel.file.config.FileFormat;
import org.apache.seatunnel.connectors.seatunnel.file.config.FileSystemType;
import org.apache.seatunnel.connectors.seatunnel.file.oss.config.OssConfig;

import com.google.auto.service.AutoService;

@AutoService(Factory.class)
public class OssFileSinkFactory implements TableSinkFactory {
    @Override
    public String factoryIdentifier() {
        return FileSystemType.OSS.getFileSystemPluginName();
    }

    @Override
    public OptionRule optionRule() {
        return OptionRule.builder()
            .required(OssConfig.FILE_PATH)
            .required(OssConfig.BUCKET)
            .required(OssConfig.ACCESS_KEY)
            .required(OssConfig.ACCESS_SECRET)
            .required(OssConfig.ENDPOINT)
            .optional(BaseSinkConfig.FILE_FORMAT)
            .conditional(BaseSinkConfig.FILE_FORMAT, FileFormat.TEXT, BaseSinkConfig.ROW_DELIMITER,
                BaseSinkConfig.FIELD_DELIMITER)
            .optional(BaseSinkConfig.CUSTOM_FILENAME)
            .conditional(BaseSinkConfig.CUSTOM_FILENAME, true, BaseSinkConfig.FILE_NAME_EXPRESSION,
                BaseSinkConfig.FILENAME_TIME_FORMAT)
            .optional(BaseSinkConfig.HAVE_PARTITION)
            .conditional(BaseSinkConfig.HAVE_PARTITION, true, BaseSinkConfig.PARTITION_BY,
                BaseSinkConfig.PARTITION_DIR_EXPRESSION, BaseSinkConfig.IS_PARTITION_FIELD_WRITE_IN_FILE)
            .optional(BaseSinkConfig.SINK_COLUMNS)
            .optional(BaseSinkConfig.IS_ENABLE_TRANSACTION)
            .optional(BaseSinkConfig.DATE_FORMAT)
            .optional(BaseSinkConfig.DATETIME_FORMAT)
            .optional(BaseSinkConfig.TIME_FORMAT)
            .build();
    }
}
