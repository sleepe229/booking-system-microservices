/**
 * Audit Module - управление логами аудита
 */

async function loadAuditLogs() {
    const filters = {
        eventType: document.getElementById('auditEventType').value || null
    };

    try {
        const logs = await AUDIT_API.getLogs(filters);
        displayAuditLogs(logs);
    } catch (e) {
        console.error('Error loading audit logs:', e);
        alert('Error: ' + e.message);
    }
}

function displayAuditLogs(logs) {
    const container = document.getElementById('auditList');

    if (!logs || logs.length === 0) {
        container.innerHTML = '<p>No audit logs found</p>';
        return;
    }

    let html = `
            <table>
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Event Type</th>
                        <th>Timestamp</th>
                        <th>Booking ID</th>
                        <th>Customer Email</th>
                        <th>Event Data</th>
                    </tr>
                </thead>
                <tbody>
        `;

    logs.forEach(log => {
        html += `
                <tr>
                    <td>${log.id}</td>
                    <td>${log.eventType}</td>
                    <td>${new Date(log.timestamp).toLocaleString()}</td>
                    <td>${log.bookingId || '-'}</td>
                    <td>${log.customerEmail || '-'}</td>
                    <td><small>${log.eventData?.substring(0, 50)}...</small></td>
                </tr>
            `;
    });

    html += `
                </tbody>
            </table>
        `;

    container.innerHTML = html;
}

async function exportAuditCSV() {
    const startDate = document.getElementById('auditStartDate').value;
    const endDate = document.getElementById('auditEndDate').value;
    const eventType = document.getElementById('auditEventType').value;

    const url = AUDIT_API.exportCSV({
        startDate: startDate ? startDate + 'T00:00:00' : null,
        endDate: endDate ? endDate + 'T23:59:59' : null,
        eventType: eventType || null
    });

    const link = document.createElement('a');
    link.href = url;
    link.download = 'audit_logs.csv';
    link.click();
}

function exportAuditJSON() {
    const startDate = document.getElementById('auditStartDate').value;
    const endDate = document.getElementById('auditEndDate').value;
    const eventType = document.getElementById('auditEventType').value;

    const filters = {
        startDate: startDate ? startDate + 'T00:00:00' : null,
        endDate: endDate ? endDate + 'T23:59:59' : null,
        eventType: eventType || null
    };

    AUDIT_API.getLogs(filters).then(logs => {
        const json = JSON.stringify(logs, null, 2);
        const blob = new Blob([json], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = 'audit_logs.json';
        link.click();
    });
}

