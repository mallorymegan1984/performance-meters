/*
 * Copyright (c)2005-2010 Mark Logic Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of the Apache License does not indicate that this project is
 * affiliated with the Apache Software Foundation.
 */
package com.marklogic.performance;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Random;

import com.marklogic.performance.reporter.Reporter;
import com.marklogic.performance.reporter.XMLReporter;
import com.marklogic.performance.sampler.HTTPSampler;
import com.marklogic.performance.sampler.Sampler;
import com.marklogic.performance.sampler.URISampler;
import com.marklogic.performance.sampler.XCCSampler;

/**
 * @author Ron Avnur, ron.avnur@marklogic.com
 * @author Michael Blakeley, michael.blakeley@marklogic.com
 * @author Wayne Feick, wayne.feick@marklogic.com
 * 
 */
public class Configuration {

    /**
     * 
     */
    public static final String TEST_TIME_KEY = "testTime";

    /**
     * 
     */
    public static final String REPORT_STANDARD_DEVIATION_KEY = "reportStandardDeviation";

    /**
     * 
     */
    public static final String REPORTER_DEFAULT = XMLReporter.class
            .getCanonicalName();

    /**
     * 
     */
    public static final String REPORTER_KEY = "reporter";

    /**
     * 
     */
    public static final String RANDOM_SEED_KEY = "randomSeed";

    /**
     * 
     */
    public static final String IS_RANDOM_TEST_DEFAULT = "false";

    /**
     * 
     */
    public static final String IS_RANDOM_TEST_KEY = "isRandomTest";

    public static final boolean RECORDRESULTS_DEFAULT = false;

    public static final boolean SHARED_DEFAULT = false;

    public static final String PROTOCOL_DEFAULT = "http";

    public static final String HOST_DEFAULT = "localhost";

    public static final int PORT_DEFAULT = 8003;

    public static final String USER_DEFAULT = "admin";

    public static final String PASSWORD_DEFAULT = "admin";

    public static final long RANDOMSEED_DEFAULT = 0;

    public static final long TESTTIME_DEFAULT = 0;

    public static final boolean CHECKRESULTS_DEFAULT = false;

    public static final boolean REPORTTIME_DEFAULT = true;

    public static final String SAMPLERCLASS_KEY = "samplerClass";

    public static final String TESTLISTCLASS_KEY = "testListClass";

    public static final String TESTLISTCLASS_DEFAULT = XMLFileTestList.class
            .getName();

    public static final String ELEMENTQNAME_KEY = "elementQName";

    public static final int READSIZE_DEFAULT = 32 * 1024;

    public static final boolean REPORTSTDDEV_DEFAULT = false;

    public static final long MILLIS_PER_SECOND = 1000;

    public static final long NANOS_PER_MILLI = 1000 * 1000;

    public static final long NANOS_PER_SECOND = 1000 * 1000 * 1000;

    public static final String PROTOCOL_HTTPS = "https";

    public static final String THREAD_NAME_PREFIX = "sampler-";

    public static final String THINK_MILLIS_KEY = "thinkMillis";

    public static final long THINK_MILLIS_DEFAULT = 0;

    private String protocol;

    private String[] host;

    private int port;

    private String user;

    private String password;

    private String inputPath;

    private String outputPath;

    private int numThreads;

    private boolean reportTime;

    // output results even if reporting time
    private boolean recordResults;

    private String testType;

    // shared
    // if true, then threads share the input file tests
    // if false, then each thread will process all tests, starting an different
    // offsets
    private boolean shared;

    private String reporterClassName;

    private long randomSeed = RANDOMSEED_DEFAULT;

    private boolean isRandomTest = false;

    private boolean isTimedTest = false;

    private int readSize = READSIZE_DEFAULT;

    private long testTime;

    private int[] reportDurationPercentiles;

    private boolean checkResults = CHECKRESULTS_DEFAULT;

    private Random random;

    private int hostIndex = 0;

    private Properties props;

    private String testListClassName;

    private String samplerClassName;

    private String elementQName;

    private boolean reportStandardDeviation = REPORTSTDDEV_DEFAULT;

    private long thinkMillis = THINK_MILLIS_DEFAULT;

    public Configuration(String[] paths, boolean loadSystemProperties)
            throws IOException {
        // set up the initial object using a set of paths, plus system
        // properties
        // if the user wants to supply more properties,
        // the load() method is public.
        props = new Properties();

        if (paths.length > 0) {
            for (int i = 0; i < paths.length; i++) {
                props.load(new FileInputStream(paths[i]));
            }
        }

        if (loadSystemProperties) {
            load(System.getProperties());
        }
    }

    // for unit testing
    public Configuration(Properties _props) {
        props = new Properties();
        load(_props);
    }

    /**
     * @param props
     */
    public void load(Properties _props) {
        // TODO warn about unexpected properties

        // fill in the config from the supplied properties object:
        // allow it to override any existing properties
        props.putAll(_props);

        // we support multiple hosts, but they must all be on the same port:
        // this would be easier with multiple connection strings...
        String hostString = props.getProperty("host", HOST_DEFAULT);
        host = hostString.split("\\s+");

        // TODO connection-string would be simpler for xcc
        port = Integer.parseInt(props.getProperty("port", ""
                + PORT_DEFAULT));

        protocol = props.getProperty("protocol", PROTOCOL_DEFAULT);

        user = props.getProperty("user", USER_DEFAULT);

        password = props.getProperty("password", PASSWORD_DEFAULT);

        readSize = Integer.parseInt(props.getProperty("readSize", ""
                + READSIZE_DEFAULT));

        inputPath = props.getProperty("inputPath");

        // default to cwd for output files
        outputPath = props.getProperty("outputPath", "");

        numThreads = Integer.parseInt(props
                .getProperty("numThreads", "1"));

        reportTime = Boolean.valueOf(
                props.getProperty("reportTime", "" + REPORTTIME_DEFAULT))
                .booleanValue();

        // support multiple percentiles: CSV or SSV
        String percentileString = props
                .getProperty("reportPercentileDuration");
        if (percentileString == null) {
            reportDurationPercentiles = null;
        } else {
            String[] percentileStringArray = percentileString
                    .split("[,\\s]+");
            reportDurationPercentiles = new int[percentileStringArray.length];
            for (int i = 0; i < percentileStringArray.length; i++) {
                reportDurationPercentiles[i] = Integer
                        .parseInt(percentileStringArray[i]);
            }
        }

        // support standard deviation
        reportStandardDeviation = Boolean.valueOf(
                props.getProperty(REPORT_STANDARD_DEVIATION_KEY, ""
                        + REPORTSTDDEV_DEFAULT)).booleanValue();

        testTime = Long.parseLong(props.getProperty(TEST_TIME_KEY, ""
                + TESTTIME_DEFAULT));

        isTimedTest = (testTime > 0);

        isRandomTest = Boolean.valueOf(
                props.getProperty(IS_RANDOM_TEST_KEY,
                        IS_RANDOM_TEST_DEFAULT)).booleanValue();

        thinkMillis = Long.parseLong(props.getProperty(THINK_MILLIS_KEY,
                "" + THINK_MILLIS_DEFAULT));

        // for backward compatibility
        recordResults = Boolean.valueOf(
                props.getProperty("recordResults", props.getProperty(
                        "forceResults", "" + RECORDRESULTS_DEFAULT)))
                .booleanValue();

        checkResults = Boolean.valueOf(
                props.getProperty("checkResults", ""
                        + CHECKRESULTS_DEFAULT)).booleanValue();

        if (checkResults && !recordResults) {
            // this doesn't make any sense, so don't honor it
            System.err
                    .println("WARNING: checkResults=true, recordResults=false!\n"
                            + "WARNING: turning off checkResults!");
            checkResults = false;
        }

        shared = Boolean.valueOf(
                props.getProperty("shared", "" + SHARED_DEFAULT))
                .booleanValue();

        // determine which sampler to use
        String testType = props.getProperty("testType", "XCC");

        reporterClassName = props.getProperty(REPORTER_KEY,
                REPORTER_DEFAULT);
        // System.err.println("reporterClassName = " + reporterClassName);

        if (reporterClassName.indexOf('.') < 1) {
            // prepend the reporter package name
            reporterClassName = Reporter.class.getPackage().getName()
                    + "." + reporterClassName;
        }

        // no default value for sampler class name
        samplerClassName = props.getProperty(SAMPLERCLASS_KEY);
        if (null != samplerClassName) {
            if (samplerClassName.indexOf('.') < 1) {
                // prepend the abstract class's package name
                samplerClassName = Sampler.class.getPackage().getName()
                        + "." + samplerClassName;
            }
        } else if ("URI".equalsIgnoreCase(testType)) {
            samplerClassName = URISampler.class.getName();
        } else if ("HTTP".equalsIgnoreCase(testType)) {
            samplerClassName = HTTPSampler.class.getName();
        } else {
            // default
            samplerClassName = XCCSampler.class.getName();
        }

        // handle test list class plug-in
        testListClassName = props.getProperty(TESTLISTCLASS_KEY,
                TESTLISTCLASS_DEFAULT);
        // System.err.println("testListClassName = " + testListClassName);
        if (testListClassName.indexOf('.') < 1) {
            // prepend package name
            testListClassName = TestList.class.getPackage().getName()
                    + "." + testListClassName;
        }

        randomSeed = Long.parseLong(props.getProperty(RANDOM_SEED_KEY, ""
                + RANDOMSEED_DEFAULT));
        if (randomSeed != RANDOMSEED_DEFAULT
                && !(isTimedTest && isRandomTest)) {
            // this doesn't make any sense, so don't honor it
            System.err
                    .println("WARNING: tried to set randomSeed with timedTest="
                            + isTimedTest
                            + ", randomTest="
                            + isRandomTest
                            + "!\n"
                            + "WARNING: turning off randomSeed!");
            randomSeed = RANDOMSEED_DEFAULT;
        }

        if (randomSeed != RANDOMSEED_DEFAULT) {
            System.err.println("using base random seed: " + randomSeed);
            // random tests only make sense as timed tests
            random = new Random();
            random.setSeed(randomSeed);
        }

        // no default value
        elementQName = props.getProperty(ELEMENTQNAME_KEY);
    }

    public String configString() {
        return "-Dprotocol=" + protocol + " -Dhost=" + host[0]
                + " -Dport=" + port + " -Duser=" + user + " -Dpassword="
                + password + " -DinputPath=" + inputPath
                + " -DoutputPath=" + outputPath + " -DnumThreads="
                + numThreads + " -Dshared=" + shared + " -DreportTime="
                + reportTime + " -DrecordResults=" + recordResults
                + " -DthinkMillis=" + thinkMillis
                + " -DtestType=" + testType;
    }

    public String getHost() {
        if (host == null) {
            return null;
        }

        if (host.length < 2) {
            return host[0];
        }
        // round-robin across available hosts
        return host[hostIndex++ % host.length];
    }

    public String getProtocol() {
        return protocol;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    String getInputPath() {
        return inputPath;
    }

    String getOutputPath() {
        return outputPath;
    }

    int getNumThreads() {
        return numThreads;
    }

    public int getPort() {
        return port;
    }

    public boolean isReportTime() {
        return reportTime;
    }

    public long getRandomSeed() {
        return randomSeed;
    }

    public boolean getRecordResults() {
        return recordResults;
    }

    public boolean isShared() {
        return shared;
    }

    /**
     * @return
     */
    public String getReporterClassName() {
        return reporterClassName;
    }

    /**
     * @return
     */
    public boolean isRandomTest() {
        return isRandomTest;
    }

    /**
     * @return
     */
    public boolean isTimedTest() {
        return isTimedTest;
    }

    /**
     * @return
     */
    public int getReadSize() {
        return readSize;
    }

    public long getTestTime() {
        return testTime;
    }

    public long getTestTimeNanos() {
        return NANOS_PER_SECOND * testTime;
    }

    /**
     * @return
     */
    public boolean hasReportPercentileDuration() {
        return reportDurationPercentiles != null
                && reportDurationPercentiles.length > 0;
    }

    /**
     * @return
     */
    public int[] getReportPercentileDuration() {
        return reportDurationPercentiles;
    }

    /**
     * @return
     */
    public boolean checkResults() {
        return checkResults;
    }

    /**
     * @return
     */
    public String getTestListClassName() {
        return testListClassName;
    }

    /**
     * @return
     */
    public String getElementQName() {
        return elementQName;
    }

    public boolean isReportStandardDeviation() {
        return reportStandardDeviation;
    }

    /**
     * @return
     */
    public String getSamplerClassName() {
        return samplerClassName;
    }

    /**
     * @return
     */
    public long getThinkMillis() {
        return thinkMillis;
    }

}
