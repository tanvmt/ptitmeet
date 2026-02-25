import api from './api';

export const meetingService = {
    createInstantMeeting: async () => {
        const response = await api.post('/meetings/instant');
        return response.data.data; 
    },

    joinMeeting: async (meetingCode, password = null) => {
        const response = await api.post(`/meetings/${meetingCode}/join`, { password });
        return response.data.data; 
    },

    getMeetingInfo: async (meetingCode) => {
        const response = await api.get(`/meetings/${meetingCode}/info`);
        return response.data.data;
    },

    getWaitingList: async (meetingCode) => {
        const response = await api.get(`/meetings/${meetingCode}/waiting-room`);
        return response.data.data;
    },

    processApproval: async (meetingCode, participantId, action) => {
        const response = await api.post(`/meetings/${meetingCode}/approval`, { 
            participantId, 
            action
        });
        return response.data;
    }
};