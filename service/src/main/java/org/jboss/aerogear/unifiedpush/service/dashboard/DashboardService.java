/**
 * JBoss, Home of Professional Open Source
 * Copyright Red Hat, Inc., and individual contributors.
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
package org.jboss.aerogear.unifiedpush.service.dashboard;

import org.jboss.aerogear.unifiedpush.api.PushApplication;
import org.jboss.aerogear.unifiedpush.api.PushMessageInformation;
import org.jboss.aerogear.unifiedpush.api.Variant;
import org.jboss.aerogear.unifiedpush.dao.InstallationDao;
import org.jboss.aerogear.unifiedpush.dao.PushApplicationDao;
import org.jboss.aerogear.unifiedpush.dao.PushMessageInformationDao;

import javax.inject.Inject;
import java.util.*;

/**
 * Class for loading various data for the Dashboard of the Admin UI
 */
public class DashboardService {

    @Inject
    private PushApplicationDao pushApplicationDao;
    @Inject
    private InstallationDao installationDao;
    @Inject
    private PushMessageInformationDao pushMessageInformationDao;


    /**
     * Receives the dashboard data for the given user
     */
    public DashboardData loadDashboardData(String principalName) {

        long totalApps = totalApplicationNumber(principalName);
        long totalDevices = totalDeviceNumber(principalName);
        long totalMessages = totalMessages(principalName);


        final DashboardData data = new DashboardData();
        data.setApplications(totalApps);
        data.setDevices(totalDevices);
        data.setMessages(totalMessages);

        return data;
    }

    /**
     * Loads all the Variant objects where we did notice some failures on sending
     * for the given user
     */
    public List<ApplicationVariant> getVariantsWithWarnings(String principalName) {
        final List<String> warningIDs = pushMessageInformationDao.findVariantIDsWithWarnings(principalName);
        if (warningIDs.isEmpty()) {
            return Collections.emptyList();
        }

        return wrapApplicationVariant(pushApplicationDao.findByVariantIds(warningIDs));
    }

    /**
     * Loads all the Variant objects with the most received messages
     */
    public List<Application> getTopThreeLastActivity(String principalName) {
        return wrapApplication(pushMessageInformationDao.findLastThreeActivity(principalName));
    }

    private List<ApplicationVariant> wrapApplicationVariant(List<PushApplication> applications) {
        final List<ApplicationVariant> applicationVariants = new ArrayList<ApplicationVariant>(applications.size());
        for (PushApplication application : applications) {
            for (Variant variant : application.getVariants()) {
                final ApplicationVariant applicationVariant = new ApplicationVariant(application, variant);
                applicationVariants.add(applicationVariant);
            }
        }
        return applicationVariants;
    }

    private List<Application> wrapApplication(List<PushMessageInformation> pushMessageInformations) {
        final List<Application> applications= new ArrayList<Application>(pushMessageInformations.size());
        for (PushMessageInformation pushMessageInformation : pushMessageInformations) {
            String applicationName = pushApplicationDao.findByPushApplicationID(pushMessageInformation.getPushApplicationId()).getName();
            final Application application= new Application(applicationName, pushMessageInformation.getPushApplicationId(), pushMessageInformation.getTotalReceivers(),pushMessageInformation.getSubmitDate());
            applications.add(application);

        }
        return applications;
    }

    private long totalMessages(String principalName) {
        return pushMessageInformationDao.getNumberOfPushMessagesForApplications(principalName);
    }

    private long totalDeviceNumber(String principalName) {
        return installationDao.getNumberOfDevicesForVariantIDs(principalName);
    }

    private long totalApplicationNumber(String principalName) {
        return  pushApplicationDao.getNumberOfPushApplicationsForDeveloper(principalName);
    }
}
