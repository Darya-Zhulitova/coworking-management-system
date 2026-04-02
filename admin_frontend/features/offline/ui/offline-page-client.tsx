'use client';

import Link from 'next/link';
import Button from 'react-bootstrap/Button';
import Card from 'react-bootstrap/Card';
import CardBody from 'react-bootstrap/CardBody';
import CardText from 'react-bootstrap/CardText';
import CardTitle from 'react-bootstrap/CardTitle';
import Container from 'react-bootstrap/Container';
import Stack from 'react-bootstrap/Stack';

export function OfflinePageClient() {
    return (
        <main className="page-shell d-flex align-items-center">
            <Container className="py-5" style={{ maxWidth: 760 }}>
                <Card className="content-card">
                    <CardBody className="p-4 p-lg-5">
                        <Stack gap={3}>
                            <div>
                                <CardTitle as="h1" className="mb-2">
                                    You are offline
                                </CardTitle>
                                <CardText className="mb-0 text-body-secondary">
                                    The server is currently unreachable. Check your connection and try again.
                                </CardText>
                            </div>

                            <div className="d-flex gap-2 flex-wrap">
                                <Button onClick={() => window.location.reload()}>
                                    Retry
                                </Button>
                                <Button as={Link} href="/login" variant="outline-secondary">
                                    Go to login
                                </Button>
                            </div>
                        </Stack>
                    </CardBody>
                </Card>
            </Container>
        </main>
    );
}