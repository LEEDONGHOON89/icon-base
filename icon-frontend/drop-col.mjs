import pg from 'pg';
const { Client } = pg;
const c = new Client({ host: 'localhost', port: 5432, database: 'icon', user: 'postgres', password: '1234' });
await c.connect();
try {
  const r = await c.query('ALTER TABLE ds_file_system_config DROP COLUMN IF EXISTS agent_target_config_id');
  console.log('OK:', r.command);
  const r2 = await c.query("SELECT column_name FROM information_schema.columns WHERE table_name='ds_file_system_config' AND column_name LIKE 'agent%'");
  console.log('Remaining agent columns:', r2.rows);
} finally {
  await c.end();
}
