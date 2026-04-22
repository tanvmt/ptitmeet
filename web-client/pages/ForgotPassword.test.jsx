import React from 'react';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter } from 'react-router-dom';

const { forgotPassword } = vi.hoisted(() => ({
    forgotPassword: vi.fn(),
}));

vi.mock('../services/authService', () => ({
    authService: {
        forgotPassword,
    },
}));

import ForgotPassword from './ForgotPassword';

describe('ForgotPassword', () => {
    beforeEach(() => {
        forgotPassword.mockReset();
    });

    it('shows success state after a successful request', async () => {
        forgotPassword.mockResolvedValue({ code: 1000 });

        render(
            <MemoryRouter>
                <ForgotPassword />
            </MemoryRouter>
        );

        fireEvent.change(screen.getByPlaceholderText('Địa chỉ Email'), {
            target: { value: 'demo@example.com' },
        });
        fireEvent.click(screen.getByRole('button', { name: 'Gửi liên kết khôi phục' }));

        expect(await screen.findByText('Kiểm tra Email')).toBeInTheDocument();
        expect(screen.getByText(/demo@example.com/)).toBeInTheDocument();
    });
});
